package controllers;

import dtos.file.*;
import extension.NoFileSubmittedOnUploadException;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.filemanager.FileManager;
import managers.filemanager.FilenameAlreadyExistsException;
import managers.filemanager.QuotaExceededException;
import managers.filemanager.dto.FileMeta;
import models.PermissionLevel;
import models.finders.UserQuota;
import org.apache.commons.lang3.StringUtils;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import policyenforcement.session.Authentication;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static play.libs.Scala.asScala;

@Singleton
@Authentication.Required
public class FileController extends Controller {
    private final Form<UploadFileDto> uploadFileForm;
    private final Form<DeleteFileDto> deleteFileForm;
    private final Form<SearchQueryDto> searchFileForm;
    private final Form<EditFileCommentDto> editFileCommentDtoForm;
    private final Form<EditFileContentDto> editFileContentDtoForm;


    private final SessionManager sessionManager;
    private final FileManager fileManager;

    @Inject
    public FileController(SessionManager sessionManager, FileManager fileManager, FormFactory formFactory) {
        this.uploadFileForm = formFactory.form(UploadFileDto.class);
        this.deleteFileForm = formFactory.form(DeleteFileDto.class);
        this.searchFileForm = formFactory.form(SearchQueryDto.class);
        this.editFileCommentDtoForm = formFactory.form(EditFileCommentDto.class);
        this.editFileContentDtoForm = formFactory.form(EditFileContentDto.class);

        this.sessionManager = sessionManager;
        this.fileManager = fileManager;
    }

    public Result showOwnFiles() {
        List<FileMeta> files = fileManager.ownedByCurrentUserFiles();
        return ok(views.html.file.Files.render(asScala(files)));
    }

    public Result showSharedFiles() {
        List<FileMeta> sharedFiles = fileManager.sharedByCurrentUserFiles();
        return ok(views.html.file.SharedFiles.render(asScala(sharedFiles)));
    }

    public Result showThirdPartyFiles() {
        List<FileMeta> files = fileManager.sharedWithCurrentUserFiles();
        return ok(views.html.file.Files.render(asScala(files)));
    }

    public Result deleteFile() throws UnauthorizedException, InvalidArgumentException {
        Form<DeleteFileDto> boundForm = deleteFileForm.bindFromRequest();
        if (boundForm.hasErrors()) {
            throw new InvalidArgumentException();
        }

        fileManager.deleteFile(boundForm.get().getFileId());

        return redirect(routes.FileController.showOwnFiles());
    }

    public Result showUploadFileForm() {
        List<UserPermissionDto> userPermissionDtos = this.fileManager.getUserPermissionDtosForCreate();
        List<GroupPermissionDto> groupPermissionDtos = this.fileManager.getGroupPermissionDtosForCreate();
        return ok(views.html.file.UploadFile.render(this.uploadFileForm, asScala(userPermissionDtos), asScala(groupPermissionDtos), null));
    }

    public Result showUploadFileToGroupForm(Long groupId) {
        List<UserPermissionDto> userPermissionDtos = this.fileManager.getUserPermissionDtosForCreate();
        List<GroupPermissionDto> groupPermissionDtos = this.fileManager.getGroupPermissionDtosForCreate();

        groupPermissionDtos.stream()
                .filter(x -> x.getGroupId().equals(groupId))
                .findFirst()
                .ifPresent(x -> x.setPermissionLevel(PermissionLevel.READ));

        UploadFileDto dto = new UploadFileDto();
        dto.setPreSelectedGroupId(groupId);

        return ok(views.html.file.UploadFile.render(this.uploadFileForm.fill(dto), asScala(userPermissionDtos), asScala(groupPermissionDtos), groupId));
    }

    public Result uploadFile() throws UnauthorizedException, IOException {
        Form<UploadFileDto> boundForm = uploadFileForm.bindFromRequest();
        List<UserPermissionDto> userPermissionDtos = this.fileManager.getUserPermissionDtosForCreate();
        List<GroupPermissionDto> groupPermissionDtos = this.fileManager.getGroupPermissionDtosForCreate();

        // Permissions-Eingabedaten verarbeiten
        Map<String, String> fields = boundForm.rawData();
        for (Map.Entry<String, String> entry: fields.entrySet()) {
            PermissionLevel pl = permissionLevelFromFormString(entry.getValue());
            if(pl != PermissionLevel.NONE) {
                if(entry.getKey().startsWith("user_")) {
                    String userIdString = entry.getKey().substring(5);
                    Long userId = Long.parseLong(userIdString);
                    Optional<UserPermissionDto> relevantDto = userPermissionDtos.stream().filter(x -> x.getUserId().equals(userId)).findFirst();
                    if(relevantDto.isPresent()) {
                        relevantDto.get().setPermissionLevel(pl);
                    }
                }

                else if(entry.getKey().startsWith("group_")) {
                    String groupIdString = entry.getKey().substring(6);
                    Long groupId = Long.parseLong(groupIdString);
                    Optional<GroupPermissionDto> relevantDto = groupPermissionDtos.stream().filter(x -> x.getGroupId().equals(groupId)).findFirst();
                    if(relevantDto.isPresent()) {
                        relevantDto.get().setPermissionLevel(pl);
                    }
                }
            }
        }

        if(boundForm.hasErrors()) {
            String strGroupId = boundForm.rawData().get("preSelectedGroupId");
            Long preSelectedGroupId = null;
            try {
                preSelectedGroupId = Long.valueOf(strGroupId);
            } catch (NumberFormatException e) {}

            return badRequest(views.html.file.UploadFile.render(boundForm, asScala(userPermissionDtos), asScala(groupPermissionDtos), preSelectedGroupId));
        }

        UploadFileDto uploadFileDto = boundForm.get();

        try {
            Http.MultipartFormData<java.io.File> body = request().body().asMultipartFormData();
            Http.MultipartFormData.FilePart<java.io.File> file = body.getFile("file");

            if(StringUtils.isEmpty(file.getFilename())) {
                throw new NoFileSubmittedOnUploadException();
            }

            byte[] data = Files.readAllBytes(file.getFile().toPath());

            List<UserPermissionDto> userPermissions = userPermissionDtos.stream().filter(x -> !x.getPermissionLevel().equals(PermissionLevel.NONE)).collect(Collectors.toList());
            List<GroupPermissionDto> groupPermissions = groupPermissionDtos.stream().filter(x -> !x.getPermissionLevel().equals(PermissionLevel.NONE)).collect(Collectors.toList());

            this.fileManager.createFile(uploadFileDto.getFilename(), uploadFileDto.getComment(), data, userPermissions, groupPermissions);

            return redirect(routes.FileController.showOwnFiles());
        } catch (NoFileSubmittedOnUploadException e) {
            boundForm = boundForm.withError("file", "Bitte wählen Sie eine Datei zum Upload aus.");
            return badRequest(views.html.file.UploadFile.render(boundForm, asScala(userPermissionDtos), asScala(groupPermissionDtos), uploadFileDto.getPreSelectedGroupId()));
        } catch(QuotaExceededException e) {
            boundForm = boundForm.withGlobalError("Quota überschritten. Bitte geben sie eine kleinere Datei an.");
            return badRequest(views.html.file.UploadFile.render(boundForm, asScala(userPermissionDtos), asScala(groupPermissionDtos), uploadFileDto.getPreSelectedGroupId()));
        } catch (FilenameAlreadyExistsException e) {
            boundForm = boundForm.withError("filename", "Name bereits belegt!");
            return badRequest(views.html.file.UploadFile.render(boundForm, asScala(userPermissionDtos), asScala(groupPermissionDtos), uploadFileDto.getPreSelectedGroupId()));
        }
    }

    private PermissionLevel permissionLevelFromFormString(String s) {
        switch (s) {
            case "Read":
                return PermissionLevel.READ;
            case "Write":
                return PermissionLevel.WRITE;
            case "ReadAndWrite":
                return PermissionLevel.READWRITE;
                default:
            return PermissionLevel.NONE;
        }
    }

    public Result showQuotaUsage() {
        UserQuota uq = fileManager.getCurrentQuotaUsage();
        UserQuotaDto dto = new UserQuotaDto(
                sessionManager.currentUser().getQuotaLimit(),
                uq.getNameUsage(),
                uq.getCommentUsage(),
                uq.getFileContentUsage());

        return ok(views.html.file.UserQuota.render(dto));
    }


    public Result showFile(long fileId) throws UnauthorizedException, InvalidArgumentException {
        FileMeta fileMeta = fileManager.getFileMeta(fileId);

        EditFileCommentDto fileCommentDto = new EditFileCommentDto(
            fileMeta.getFileId(),
            fileMeta.getComment()
        );

        return ok(views.html.file.File.render(fileMeta, editFileContentDtoForm, editFileCommentDtoForm.fill(fileCommentDto)));
    }


    public Result downloadFile(long fileId) throws UnauthorizedException, InvalidArgumentException {
        FileMeta fileMeta = fileManager.getFileMeta(fileId);
        byte[] data = fileManager.getFileContent(fileId);
        return ok(data).as("application/octet-stream").withHeader("Content-Disposition", "attachment; filename=\"" + fileMeta.getFilename() + "\"");
    }

    public Result editFileComment() throws UnauthorizedException, InvalidArgumentException {
        Form<EditFileCommentDto> boundForm = editFileCommentDtoForm.bindFromRequest();
        if (boundForm.hasErrors()) {
            Long fileId;

            try {
                fileId = Long.valueOf(boundForm.rawData().get("fileId"));
            } catch (NumberFormatException ex) {
                throw new InvalidArgumentException();
            }

            FileMeta fileMeta = fileManager.getFileMeta(fileId);
            return badRequest(views.html.file.File.render(fileMeta, editFileContentDtoForm, boundForm));
        }

        EditFileCommentDto data = boundForm.get();

        try {
            fileManager.editFileComment(data.getFileId(), data.getComment());
        } catch (QuotaExceededException ex) {
            boundForm = boundForm.withError("comment", "Kommentar zu lang (Quota überschritten)");
            FileMeta fileMeta = fileManager.getFileMeta(data.getFileId());
            return badRequest(views.html.file.File.render(fileMeta, editFileContentDtoForm, boundForm));
        }

        return redirect(routes.FileController.showFile(boundForm.get().getFileId()));
    }

    public Result editFileContent() throws UnauthorizedException, InvalidArgumentException, IOException {
        Form<EditFileContentDto> boundForm = editFileContentDtoForm.bindFromRequest();
        if (boundForm.hasErrors()) {
            Long fileId;

            try {
                fileId = Long.valueOf(boundForm.rawData().get("fileId"));
            } catch (NumberFormatException ex) {
                throw new InvalidArgumentException();
            }

            FileMeta fileMeta = fileManager.getFileMeta(fileId);
            return badRequest(views.html.file.File.render(fileMeta, boundForm, editFileCommentDtoForm));
        }

        EditFileContentDto data = boundForm.get();
        Http.MultipartFormData<java.io.File> body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart<java.io.File> file = body.getFile("file");

        if(StringUtils.isEmpty(file.getFilename())) {
            boundForm = boundForm.withError("file", "Bitte Datei auswählen!");
            FileMeta fileMeta = fileManager.getFileMeta(data.getFileId());
            return badRequest(views.html.file.File.render(fileMeta, boundForm, editFileCommentDtoForm));
        }

        byte[] fileData = Files.readAllBytes(file.getFile().toPath());

        try {
            fileManager.editFileContent(data.getFileId(), fileData);
        } catch (QuotaExceededException ex) {
            boundForm = boundForm.withError("file", "Datei zu groß (Quota überschritten)");
            FileMeta fileMeta = fileManager.getFileMeta(data.getFileId());
            return badRequest(views.html.file.File.render(fileMeta, boundForm, editFileCommentDtoForm));
        }

        return redirect(routes.FileController.showFile(boundForm.get().getFileId()));
    }

    public Result searchFiles(){
        Form<SearchQueryDto> boundForm = searchFileForm.bindFromRequest("query");

        if(boundForm.hasErrors()){
            return badRequest(views.html.file.SearchResult.render(asScala(new ArrayList<>()), boundForm));
        }

        String query = boundForm.get().getQuery();

        List<FileMeta> files = fileManager.searchFile(query);
        return ok(views.html.file.SearchResult.render(asScala(files), boundForm));
    }

}
