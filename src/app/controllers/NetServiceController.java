package controllers;

import dtos.netservice.CreateNetServiceCredentialsDto;
import dtos.netservice.CreateNetServiceDto;
import dtos.netservice.DeleteNetServiceDto;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.netservicemanager.NetServiceManager;
import models.NetService;
import models.NetServiceCredential;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Result;
import policyenforcement.session.Authentication;
import views.html.netservice.CreateNetServiceCredential;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static play.libs.Scala.asScala;
import static play.mvc.Results.*;

@Singleton
@Authentication.Required
public class NetServiceController {

    private final NetServiceManager netServiceManager;
    private final Form<CreateNetServiceDto> createNetServiceDtoForm;
    private final Form<DeleteNetServiceDto> deleteNetServiceDtoForm;
    private final Form<CreateNetServiceCredentialsDto> createNetServiceCredentialsDtoForm;

    @Inject
    public NetServiceController(NetServiceManager netServiceManager, FormFactory formFactory){
        this.netServiceManager = netServiceManager;
        this.createNetServiceDtoForm = formFactory.form(CreateNetServiceDto.class);
        this.deleteNetServiceDtoForm = formFactory.form(DeleteNetServiceDto.class);
        this.createNetServiceCredentialsDtoForm = formFactory.form(CreateNetServiceCredentialsDto.class);
    }



    public Result showAllNetServices() throws UnauthorizedException {
        List<NetService> netServices = netServiceManager.getAllNetServices();
        return ok(views.html.netservice.NetServices.render(asScala(netServices)));
    }
    
    
    public Result deleteNetService() throws UnauthorizedException, InvalidArgumentException {
        Form<DeleteNetServiceDto> boundForm = deleteNetServiceDtoForm.bindFromRequest();

        if(boundForm.hasErrors()){
            return redirect(routes.NetServiceController.showAllNetServices());
        }

        netServiceManager.deleteNetService(boundForm.get().getNetServiceId());
        return redirect(routes.NetServiceController.showAllNetServices());
    }
    
    public Result showAddNetServiceForm(){
        return ok(views.html.netservice.CreateNetService.render(createNetServiceDtoForm));
    }
    
    public Result createNetService() throws UnauthorizedException {
        Form<CreateNetServiceDto> boundForm = createNetServiceDtoForm.bindFromRequest();

        if(boundForm.hasErrors())
        {
            return badRequest(views.html.netservice.CreateNetService.render(boundForm));
        }

        netServiceManager.createNetService(boundForm.get().getName());
        return redirect(routes.NetServiceController.showAllNetServices());
    }


    public Result showUserNetServiceCredentials(){

        List<NetServiceCredential> credentials = netServiceManager.getUserNetServiceCredentials();
        return ok(views.html.netservice.NetServiceCredentials.render(asScala(credentials)));
    }
    public Result showCreateNetServiceCredentialForm() throws UnauthorizedException {
        return ok(views.html.netservice.CreateNetServiceCredential.render(asScala(netServiceManager.getAllNetServices()), createNetServiceCredentialsDtoForm));
    }

    public Result createNetServiceCredential() throws UnauthorizedException {
        Form<CreateNetServiceCredentialsDto> boundForm = createNetServiceCredentialsDtoForm.bindFromRequest();

        if(boundForm.hasErrors()){
            return badRequest(views.html.netservice.CreateNetServiceCredential.render(asScala(netServiceManager.getAllNetServices()),boundForm));
        }

        netServiceManager.createNetUserCredential(boundForm.get().getNetServiceId(), boundForm.get().getUsername(), boundForm.get().getPassword());


        return redirect(routes.NetServiceController.showUserNetServiceCredentials());
    }


    public Result deleteNetServiceCredential(){
        return play.mvc.Results.TODO;
    }
}
