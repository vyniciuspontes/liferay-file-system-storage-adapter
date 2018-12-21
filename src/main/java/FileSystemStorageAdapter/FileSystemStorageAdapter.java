package FileSystemStorageAdapter;

import com.liferay.counter.kernel.service.CounterLocalService;
import com.liferay.dynamic.data.mapping.io.DDMFormValuesJSONDeserializer;
import com.liferay.dynamic.data.mapping.io.DDMFormValuesJSONSerializer;
import com.liferay.dynamic.data.mapping.model.DDMStorageLink;
import com.liferay.dynamic.data.mapping.model.DDMStructureVersion;
import com.liferay.dynamic.data.mapping.service.DDMStorageLinkLocalService;
import com.liferay.dynamic.data.mapping.service.DDMStructureVersionLocalService;
import com.liferay.dynamic.data.mapping.storage.BaseStorageAdapter;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.storage.StorageAdapter;
import com.liferay.dynamic.data.mapping.validator.DDMFormValuesValidator;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.PortalUtil;

import java.io.File;
import java.io.IOException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author vyniciuspontes
 */
@Component(immediate = true, property = {
		// TODO enter required service properties
}, service = StorageAdapter.class)
public class FileSystemStorageAdapter extends BaseStorageAdapter {

	@Activate
	void activate() throws Exception {

		System.out.println("Activating " + this.getDescription());
	}

	public String getDescription() {

		return this.getClass().getSimpleName();
	}

	@Override
	public String getStorageType() {
		// TODO Auto-generated method stub
		return "File System Storage";
	}

	@Override
	protected long doCreate(long companyId, long ddmStructureId, DDMFormValues ddmFormValues,
			ServiceContext serviceContext) throws Exception {

		validate(ddmFormValues, serviceContext);

		long fileId = _counterLocalService.increment();

		DDMStructureVersion ddmStructureVersion = _ddmStructureVersionLocalService
				.getLatestStructureVersion(ddmStructureId);

		long classNameId = PortalUtil.getClassNameId(FileSystemStorageAdapter.class.getName());

		_ddmStorageLinkLocalService.addStorageLink(classNameId, fileId, ddmStructureVersion.getStructureVersionId(),
				serviceContext);

		saveFile(ddmStructureVersion.getStructureVersionId(), fileId, ddmFormValues);

		return fileId;
	}

	@Reference
	private CounterLocalService _counterLocalService;

	@Reference
	private DDMStorageLinkLocalService _ddmStorageLinkLocalService;

	@Reference
	private DDMStructureVersionLocalService _ddmStructureVersionLocalService;

	private File getFile(long structureId, long fileId) {

		System.out.println("Folder: " + getStructureFolder(structureId) + " - File: " + String.valueOf(fileId));

		return new File(getStructureFolder(structureId), String.valueOf(fileId));
	}

	private File getStructureFolder(long structureId) {
		return new File(String.valueOf(structureId));
	}

	private void saveFile(long structureVersionId, long fileId, DDMFormValues formValues) throws IOException {

		String serializedDDMFormValues = _ddmFormValuesJSONSerializer.serialize(formValues);

		File formEntryFile = getFile(structureVersionId, fileId);

		FileUtil.write(formEntryFile, serializedDDMFormValues);
	}

	@Reference
	private DDMFormValuesJSONSerializer _ddmFormValuesJSONSerializer;

	@Override
	protected void doDeleteByClass(long classPK) throws Exception {
		DDMStorageLink storageLink = _ddmStorageLinkLocalService.getClassStorageLink(classPK);

		FileUtil.delete(getFile(storageLink.getStructureId(), classPK));

		_ddmStorageLinkLocalService.deleteClassStorageLink(classPK);
	}

	@Override
	protected void doDeleteByDDMStructure(long ddmStructureId) throws Exception {

		FileUtil.deltree(getStructureFolder(ddmStructureId));

		_ddmStorageLinkLocalService.deleteStructureStorageLinks(ddmStructureId);
	}

	@Override
	protected DDMFormValues doGetDDMFormValues(long classPK) throws Exception {

		System.out.println("ClassPK: " + classPK);

		DDMStorageLink storageLink = _ddmStorageLinkLocalService.getClassStorageLink(classPK);

		DDMStructureVersion structureVersion = _ddmStructureVersionLocalService
				.getStructureVersion(storageLink.getStructureVersionId());

		String serializedDDMFormValues = FileUtil.read(getFile(structureVersion.getStructureVersionId(), classPK));

		return _ddmFormValuesJSONDeserializer.deserialize(structureVersion.getDDMForm(), serializedDDMFormValues);
	}

	@Reference
	private DDMFormValuesJSONDeserializer _ddmFormValuesJSONDeserializer;

	@Override
	protected void doUpdate(long classPK, DDMFormValues ddmFormValues, ServiceContext serviceContext) throws Exception {

		validate(ddmFormValues, serviceContext);

		DDMStorageLink storageLink = _ddmStorageLinkLocalService.getClassStorageLink(classPK);

		saveFile(storageLink.getStructureVersionId(), storageLink.getClassPK(), ddmFormValues);

	}

	protected void validate(DDMFormValues ddmFormValues, ServiceContext serviceContext) throws Exception {

		boolean validateDDMFormValues = GetterUtil.getBoolean(serviceContext.getAttribute("validateDDMFormValues"),
				true);

		if (!validateDDMFormValues) {
			return;
		}

		_ddmFormValuesValidator.validate(ddmFormValues);
	}

	@Reference
	private DDMFormValuesValidator _ddmFormValuesValidator;

}