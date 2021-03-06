package controllers;

import dtos.group.UserIdDto;
import dtos.netservice.*;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.netservicemanager.NetServiceAlreadyExistsException;
import managers.netservicemanager.NetServiceManager;
import managers.netservicemanager.PlaintextCredential;
import models.NetService;
import models.NetServiceCredential;
import play.data.Form;
import play.data.FormFactory;
import play.libs.Json;
import play.mvc.Result;
import policyenforcement.session.Authentication;
import policyenforcement.session.SessionManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

import static play.libs.Scala.asScala;
import static play.mvc.Results.*;

@Singleton
@Authentication.Required
public class NetServiceController {

    private final SessionManager sessionManager;
    private final NetServiceManager netServiceManager;
    private final Form<CreateNetServiceDto> createNetServiceDtoForm;
    private final Form<DeleteNetServiceDto> deleteNetServiceDtoForm;
    private final Form<EditNetserviceDto> editNetserviceDtoForm;
    private final Form<AddNetServiceParameterDto> addNetServiceParameterDtoForm;
    private final Form<RemoveNetServiceParameterDto> removeNetServiceParameterDtoForm;
    private final Form<CreateNetServiceCredentialsDto> createNetServiceCredentialsDtoForm;
    private final Form<DeleteNetServiceCredentialsDto> deleteNetServiceCredentialsDtoForm;
    private final Form<DeleteNetServiceDto> confirmNetServiceDeleteForm;
    private final Form<DecryptNetServiceCredentialsDto> decryptNetServiceCredentialsForm;

    @Inject
    public NetServiceController(SessionManager sessionManager, NetServiceManager netServiceManager, FormFactory formFactory) {
        this.sessionManager = sessionManager;
        this.netServiceManager = netServiceManager;
        this.createNetServiceDtoForm = formFactory.form(CreateNetServiceDto.class);
        this.deleteNetServiceDtoForm = formFactory.form(DeleteNetServiceDto.class);
        this.editNetserviceDtoForm = formFactory.form(EditNetserviceDto.class);
        this.addNetServiceParameterDtoForm = formFactory.form(AddNetServiceParameterDto.class);
        this.removeNetServiceParameterDtoForm = formFactory.form(RemoveNetServiceParameterDto.class);
        this.createNetServiceCredentialsDtoForm = formFactory.form(CreateNetServiceCredentialsDto.class);
        this.deleteNetServiceCredentialsDtoForm = formFactory.form(DeleteNetServiceCredentialsDto.class);
        this.confirmNetServiceDeleteForm = formFactory.form(DeleteNetServiceDto.class);
        this.decryptNetServiceCredentialsForm = formFactory.form(DecryptNetServiceCredentialsDto.class);
    }


    public Result showAllNetServices() throws UnauthorizedException {
        if(!sessionManager.currentPolicy().canSeeNetServiceOverviewPage())
            throw new UnauthorizedException();

        List<NetService> netServices = netServiceManager.getAllNetServices();
        return ok(views.html.netservice.NetServices.render(asScala(netServices)));
    }

    public Result showAddNetServiceForm() throws UnauthorizedException {
        if(!sessionManager.currentPolicy().canCreateNetService()){
            throw new UnauthorizedException();
        }
        return ok(views.html.netservice.CreateNetService.render(createNetServiceDtoForm));
    }

    public Result createNetService() throws UnauthorizedException {
        Form<CreateNetServiceDto> boundForm = createNetServiceDtoForm.bindFromRequest();

        if (boundForm.hasErrors()) {
            return badRequest(views.html.netservice.CreateNetService.render(boundForm));
        }

        CreateNetServiceDto dto = boundForm.get();
        NetService netService = null;
        try {
            netService = netServiceManager.createNetService(dto.getName(), dto.getUrl());
        } catch (NetServiceAlreadyExistsException e) {
            boundForm = boundForm.withError("name", "Existiert bereits!");
            return badRequest(views.html.netservice.CreateNetService.render(boundForm));
        }


        return redirect(routes.NetServiceController.showEditNetService(netService.getNetServiceId()));
    }

    public Result showEditNetService(Long netServiceId) throws UnauthorizedException, InvalidArgumentException {
        if(!sessionManager.currentPolicy().canCreateNetService()){
            throw new UnauthorizedException();
        }

        NetService netService = netServiceManager.getNetService(netServiceId);

        AddNetServiceParameterDto addNetServiceParameterDto = new AddNetServiceParameterDto();
        addNetServiceParameterDto.setNetServiceId(netServiceId);

        EditNetserviceDto editNetserviceDto = new EditNetserviceDto(netServiceId,
                netService.getName(), netService.getUrl()
        );

        return ok(views.html.netservice.EditNetService.render(netService, addNetServiceParameterDtoForm.fill(addNetServiceParameterDto), editNetserviceDtoForm.fill(editNetserviceDto)));
    }


    public Result editNetService() throws UnauthorizedException, InvalidArgumentException {
        Form<EditNetserviceDto> boundForm = editNetserviceDtoForm.bindFromRequest();

        if (boundForm.hasErrors()) {
            String netServiceId = boundForm.rawData().get("netServiceId");
            try{
                return showEditNetService(Long.parseLong(netServiceId));
            }catch (NumberFormatException e){
                throw new InvalidArgumentException();
            }
        }

        netServiceManager.editNetService(boundForm.get().getNetServiceId(), boundForm.get().getName(), boundForm.get().getUrl());

        return showEditNetService(boundForm.get().getNetServiceId());
    }

    public Result addNetServiceParameter() throws UnauthorizedException, InvalidArgumentException {
        Form<AddNetServiceParameterDto> boundForm = addNetServiceParameterDtoForm.bindFromRequest();

        if (boundForm.hasErrors()) {
            throw new InvalidArgumentException();
        }

        netServiceManager.addNetServiceParameter(boundForm.get().getNetServiceId(), boundForm.get().getParameterType(), boundForm.get().getName(), boundForm.get().getDefaultValue());
        return redirect(routes.NetServiceController.showEditNetService(boundForm.get().getNetServiceId()));
    }

    public Result removeNetServiceParameter() throws UnauthorizedException, InvalidArgumentException {
        Form<RemoveNetServiceParameterDto> boundForm = removeNetServiceParameterDtoForm.bindFromRequest();

        if (boundForm.hasErrors()) {
            throw new InvalidArgumentException();
        }

        netServiceManager.removeNetServiceParameter(boundForm.get().getNetServiceId(), boundForm.get().getNetServiceParameterId());
        return redirect(routes.NetServiceController.showEditNetService(boundForm.get().getNetServiceId()));
    }

    public Result showUserNetServiceCredentials() {
        List<NetServiceCredential> credentials = netServiceManager.getUserNetServiceCredentials();
        return ok(views.html.netservice.NetServiceCredentials.render(asScala(credentials)));
    }

    public Result showCreateNetServiceCredentialForm() throws UnauthorizedException {
        return ok(views.html.netservice.CreateNetServiceCredential.render(asScala(netServiceManager.getAllNetServices()), createNetServiceCredentialsDtoForm));
    }

    public Result createNetServiceCredential() throws UnauthorizedException, InvalidArgumentException {
        Form<CreateNetServiceCredentialsDto> boundForm = createNetServiceCredentialsDtoForm.bindFromRequest();

        if (boundForm.hasErrors()) {
            return badRequest(views.html.netservice.CreateNetServiceCredential.render(asScala(netServiceManager.getAllNetServices()), boundForm));
        }

        netServiceManager.createNetUserCredential(boundForm.get().getNetServiceId(), boundForm.get().getUsername(), boundForm.get().getPassword());
        return redirect(routes.NetServiceController.showUserNetServiceCredentials());
    }

    public Result deleteNetServiceCredential() throws UnauthorizedException, InvalidArgumentException {
        Form<DeleteNetServiceCredentialsDto> boundForm = deleteNetServiceCredentialsDtoForm.bindFromRequest();

        if (boundForm.hasErrors()) {
            throw new InvalidArgumentException();
        }

        netServiceManager.deleteNetServiceCredential(boundForm.get().getNetServiceCredentialId());

        return redirect(routes.NetServiceController.showUserNetServiceCredentials());
    }

    public Result decryptNetServiceCredential() throws UnauthorizedException, InvalidArgumentException {
        Form<DecryptNetServiceCredentialsDto> boundForm = decryptNetServiceCredentialsForm.bindFromRequest();

        if (boundForm.hasErrors()) {
            throw new InvalidArgumentException();
        }

        DecryptNetServiceCredentialsDto dto = boundForm.get();

        NetServiceCredential encryptedCredential = netServiceManager.getEncryptedCredential(dto.getCredentialId());
        PlaintextCredential plainCredential = netServiceManager.decryptCredential(encryptedCredential);

        return ok(views.html.netservice.NetServiceTrampoline.render(encryptedCredential, plainCredential));
    }

    public Result showDeleteNetServiceConfirmation() throws InvalidArgumentException, UnauthorizedException {
        Form<DeleteNetServiceDto> boundForm = deleteNetServiceDtoForm.bindFromRequest();
        if(boundForm.hasErrors()) {
            throw new InvalidArgumentException();
        }

        NetService netService = netServiceManager.getNetService(boundForm.get().getNetServiceId());

        return ok(views.html.netservice.DeleteNetServiceConfirmation.render(netService));
    }

    public Result deleteNetService() throws UnauthorizedException, InvalidArgumentException {
        Form<DeleteNetServiceDto> boundForm = confirmNetServiceDeleteForm.bindFromRequest();

        if (boundForm.hasErrors()) {
            throw new InvalidArgumentException();
        }

        netServiceManager.deleteNetService(boundForm.get().getNetServiceId());
        return redirect(routes.NetServiceController.showAllNetServices());
    }
}
