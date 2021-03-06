import extension.HashHelper;
import extension.logging.DangerousCharFilteringLogger;
import models.*;
import models.factories.UserFactory;
import org.joda.time.DateTime;
import play.Environment;
import play.Logger;
import play.db.Database;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Statement;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class DatabaseInitialization {

    private static final Logger.ALogger logger = new DangerousCharFilteringLogger(
            DatabaseInitialization.class);

    private Database db;
    private HashHelper hashHelper;
    private UserFactory userFactory;
    private Environment environment;

    @Inject
    public DatabaseInitialization(Database db, HashHelper hashHelper, UserFactory userFactory, Environment environment) {
        this.db = db;
        this.hashHelper = hashHelper;
        this.userFactory = userFactory;
        this.environment = environment;

        if(environment.isProd()) {
            prodInitialization();
        }
        if(environment.isDev() || environment.isTest()) {
            devEnvInitialization();
        }
    }

    private void prodInitialization() {
        User u1 = userFactory.CreateUser("admin", "hsh.helper+admin@gmail.com", "admin", true, 1000L);
        Group g1 = new Group("All", u1);
        Group g2 = new Group("Administrators", u1);
        g1.setIsAllGroup(true);
        g2.setIsAdminGroup(true);
        g1.setMembers(Stream.of(u1).collect(Collectors.toList()));
        g2.setMembers(Stream.of(u1).collect(Collectors.toList()));
        u1.save();
        g1.save();
        g2.save();

        NetService ns1 = new NetService();
        ns1.setName("Bibliothek");
        ns1.setUrl("https://opac.tib.eu:443/loan/DB=4/SET=2/TTL=1/USERINFO");
        ns1.getParameters().add(new NetServiceParameter("BOR_U",NetServiceParameter.NetServiceParameterType.USERNAME));
        ns1.getParameters().add(new NetServiceParameter("BOR_PW", NetServiceParameter.NetServiceParameterType.PASSWORD));
        ns1.getParameters().add(new NetServiceParameter("ACT","UI_DATA", NetServiceParameter.NetServiceParameterType.HIDDEN));
        ns1.getParameters().add(new NetServiceParameter("HOST_NAME", NetServiceParameter.NetServiceParameterType.HIDDEN));
        ns1.getParameters().add(new NetServiceParameter("HOST_PORT", NetServiceParameter.NetServiceParameterType.HIDDEN));
        ns1.getParameters().add(new NetServiceParameter("HOST_SCRIPT", NetServiceParameter.NetServiceParameterType.HIDDEN));
        ns1.getParameters().add(new NetServiceParameter("LOGIN","KNOWNUSER", NetServiceParameter.NetServiceParameterType.HIDDEN));
        ns1.getParameters().add(new NetServiceParameter("STATUS","HML_OK", NetServiceParameter.NetServiceParameterType.HIDDEN));
        ns1.save();


        NetService ns2 = new NetService();
        ns2.setName("ICMS");
        ns2.setUrl("https://icms.hs-hannover.de/qisserver/rds?state=user&type=1&category=auth.login&startpage=portal.vm");
        ns2.getParameters().add(new NetServiceParameter("asdf",NetServiceParameter.NetServiceParameterType.USERNAME));
        ns2.getParameters().add(new NetServiceParameter("fdsa", NetServiceParameter.NetServiceParameterType.PASSWORD));
        ns2.save();
    }

    private void devEnvInitialization() {
        logger.info("DatabaseInitialization - Prepare DB");

        logger.info("DatabaseInitialization - Prepare DB; Truncate all tables.");
        db.withConnection(connection -> {
            Statement stmt = connection.createStatement();
            stmt.execute("SET ALLOW_LITERALS ALL");
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            stmt.execute("CREATE CONSTANT IF NOT EXISTS ONE VALUE '1';");
            stmt.execute("ALTER TABLE users ALTER COLUMN user_id RESTART WITH ONE");
            stmt.execute("ALTER TABLE groups ALTER COLUMN group_id RESTART WITH ONE");
            stmt.execute("ALTER TABLE files ALTER COLUMN file_id RESTART WITH ONE");
            stmt.execute("ALTER TABLE group_permissions ALTER COLUMN group_permission_id RESTART WITH ONE");
            stmt.execute("ALTER TABLE user_permissions ALTER COLUMN user_permission_id RESTART WITH ONE");
            stmt.execute("ALTER TABLE netservices ALTER COLUMN net_service_id RESTART WITH ONE");
            stmt.execute("ALTER TABLE net_service_parameter ALTER COLUMN net_service_parameter_id RESTART WITH ONE");
            stmt.execute("ALTER TABLE net_service_credential ALTER COLUMN net_service_credential_id RESTART WITH ONE");

            stmt.execute("TRUNCATE TABLE groupmembers");
            stmt.execute("TRUNCATE TABLE users");
            stmt.execute("TRUNCATE TABLE groups");
            stmt.execute("TRUNCATE TABLE files");
            stmt.execute("TRUNCATE TABLE group_permissions");
            stmt.execute("TRUNCATE TABLE user_permissions");
            stmt.execute("TRUNCATE TABLE internal_session");
            stmt.execute("TRUNCATE TABLE netservices");
            stmt.execute("TRUNCATE TABLE net_service_parameter");
            stmt.execute("TRUNCATE TABLE net_service_credential");
            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
            stmt.execute("SET ALLOW_LITERALS NONE");
        });
        logger.info("DatabaseInitialization - Prepare DB; Truncated");

        logger.info("ApplicationStart - Prepare DB; Add new users and groups");
        User u1 = userFactory.CreateUser("admin", "hsh.helper+admin@gmail.com", "admin", false, 1000L);
        User u2 = userFactory.CreateUser("peter", "hsh.helper+peter@gmail.com",  "peter", false, 10L);
        User u3 = userFactory.CreateUser("klaus", "hsh.helper+klaus@gmail.com",  "klaus", false, 1000000L);
        User u4 = userFactory.CreateUser("hans", "hsh.helper+hans@gmail.com",  "hans", true, 10L);

        Group g1 = new Group("All", u1);
        Group g2 = new Group("Administrators", u1);
        Group g3 = new Group("Peter's Group", u2);

        g1.setIsAllGroup(true);
        g2.setIsAdminGroup(true);

        g1.setMembers(Stream.of(u1, u2, u3, u4).collect(Collectors.toList()));
        g2.setMembers(Stream.of(u1).collect(Collectors.toList()));
        g3.setMembers(Stream.of(u1, u2, u3).collect(Collectors.toList()));

        u1.save();
        u2.save();
        u3.save();
        u4.save();

        g1.save();
        g2.save();
        g3.save();


        File f1 = new File();
        f1.setName("admin.txt");
        f1.setComment("blablabla");
        f1.setOwner(u1);
        f1.setData(new byte[]{1,2,1,2});
        f1.setWrittenBy(u1);
        f1.setWrittenByDt(DateTime.now());
        f1.save();

        File f2 = new File();
        f2.setName("peter.txt");
        f2.setComment("blabasdasdlabla");
        f2.setOwner(u2);
        f2.setData(new byte[]{1,2,1,2});
        f2.setWrittenBy(u2);
        f2.setWrittenByDt(DateTime.now());
        f2.save();

        File f3 = new File();
        f3.setName("klaus.txt");
        f3.setComment("xaxaxxaxaxxaxaxxaxax xaxax xaxax xaxax");
        f3.setOwner(u3);
        f3.setData(new byte[]{1,2,1,2});
        f3.setWrittenBy(u3);
        f3.setWrittenByDt(DateTime.now());
        f3.save();


        File f4 = new File();
        f4.setName("admin2.txt");
        f4.setComment("blablabla");
        f4.setOwner(u1);
        f4.setData(new byte[]{1,2,1,2});
        f4.setWrittenBy(u1);
        f4.setWrittenByDt(DateTime.now());
        f4.save();


        UserPermission up1 = new UserPermission();
        up1.setUser(u2);
        up1.setFile(f3);
        up1.setCanRead(true);
        up1.setCanWrite(true);
        up1.save();

        UserPermission up2 = new UserPermission();
        up2.setUser(u1);
        up2.setFile(f3);
        up2.setCanRead(false);
        up2.setCanWrite(true);
        up2.save();

        UserPermission up3 = new UserPermission();
        up3.setUser(u2);
        up3.setFile(f1);
        up3.setCanRead(false);
        up3.setCanWrite(true);
        up3.save();

        UserPermission up4 = new UserPermission();
        up4.setUser(u2);
        up4.setFile(f4);
        up4.setCanRead(false);
        up4.setCanWrite(true);
        up4.save();

        GroupPermission gp1 = new GroupPermission();
        gp1.setGroup(g1);
        gp1.setFile(f1);
        gp1.setCanRead(true);
        gp1.setCanWrite(false);
        gp1.save();


        NetService ns1 = new NetService();
        ns1.setName("Bibliothek");
        ns1.setUrl("https://opac.tib.eu:443/loan/DB=4/SET=2/TTL=1/USERINFO");
        ns1.getParameters().add(new NetServiceParameter("BOR_U",NetServiceParameter.NetServiceParameterType.USERNAME));
        ns1.getParameters().add(new NetServiceParameter("BOR_PW", NetServiceParameter.NetServiceParameterType.PASSWORD));
        ns1.getParameters().add(new NetServiceParameter("ACT","UI_DATA", NetServiceParameter.NetServiceParameterType.HIDDEN));
        ns1.getParameters().add(new NetServiceParameter("HOST_NAME", NetServiceParameter.NetServiceParameterType.HIDDEN));
        ns1.getParameters().add(new NetServiceParameter("HOST_PORT", NetServiceParameter.NetServiceParameterType.HIDDEN));
        ns1.getParameters().add(new NetServiceParameter("HOST_SCRIPT", NetServiceParameter.NetServiceParameterType.HIDDEN));
        ns1.getParameters().add(new NetServiceParameter("LOGIN","KNOWNUSER", NetServiceParameter.NetServiceParameterType.HIDDEN));
        ns1.getParameters().add(new NetServiceParameter("STATUS","HML_OK", NetServiceParameter.NetServiceParameterType.HIDDEN));
        ns1.save();


        NetService ns2 = new NetService();
        ns2.setName("ICMS");
        ns2.setUrl("https://icms.hs-hannover.de/qisserver/rds?state=user&type=1&category=auth.login&startpage=portal.vm");
        ns2.getParameters().add(new NetServiceParameter("asdf",NetServiceParameter.NetServiceParameterType.USERNAME));
        ns2.getParameters().add(new NetServiceParameter("fdsa", NetServiceParameter.NetServiceParameterType.PASSWORD));
        ns2.save();


        logger.info("DatabaseInitialization - Prepare DB; Done adding new users and groups");
    }
}