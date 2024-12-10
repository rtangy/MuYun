package net.ximatai.muyun.platform.controller;

import io.quarkus.runtime.Startup;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import net.ximatai.muyun.ability.IChildrenAbility;
import net.ximatai.muyun.ability.ITreeAbility;
import net.ximatai.muyun.base.BaseBusinessTable;
import net.ximatai.muyun.core.exception.MyException;
import net.ximatai.muyun.database.builder.Column;
import net.ximatai.muyun.database.builder.TableWrapper;
import net.ximatai.muyun.model.ChildTableInfo;
import net.ximatai.muyun.model.PageResult;
import net.ximatai.muyun.platform.ScaffoldForPlatform;
import net.ximatai.muyun.platform.ability.IModuleRegisterAbility;
import net.ximatai.muyun.platform.model.ModuleAction;
import net.ximatai.muyun.platform.model.ModuleConfig;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

import static net.ximatai.muyun.platform.PlatformConst.BASE_PATH;
import static net.ximatai.muyun.platform.controller.RoleController.MODULE_ALIAS;

@Startup
@Tag(name = "角色管理")
@Path(BASE_PATH + "/" + MODULE_ALIAS)
public class RoleController extends ScaffoldForPlatform implements ITreeAbility, IChildrenAbility, IModuleRegisterAbility {

    public final static String MODULE_ALIAS = "role";

    @Inject
    UserRoleController userRoleController;

    @Inject
    RoleActionController roleActionController;

    @Inject
    ModuleController moduleController;

    private ChildTableInfo userRoleChild;

    private ChildTableInfo getUserRoleChild() {
        if (userRoleChild == null) {
            userRoleChild = userRoleController.toChildTable("id_at_auth_role");
        }
        return userRoleChild;
    }

    @Override
    public void onTableCreated(boolean isFirst) {
        if (isFirst) {
            this.create(
                Map.of("id", "0", "v_name", "白名单用户角色", "auth_user_role", List.of(
                    Map.of("id_at_auth_user", "0")
                ))
            );
        }
    }

    @Override
    public String getMainTable() {
        return "auth_role";
    }

    @Override
    public void fitOut(TableWrapper wrapper) {
        wrapper
            .setPrimaryKey(Column.ID_POSTGRES)
            .setInherit(BaseBusinessTable.TABLE)
            .addColumn("v_name")
            .addColumn("v_remark");
    }

    @Override
    public List<ChildTableInfo> getChildren() {

        return List.of(
            getUserRoleChild().setAutoDelete(),
            roleActionController.toChildTable("id_at_auth_role").setAutoDelete()
        );
    }

    @GET
    @Path("/availableModulesByAction/{roleID}")
    @Operation(summary = "在角色id基础上，根据功能名查询可用的模块")
    public List<Map<String, Object>> availableModulesByAction(@PathParam("roleID") String roleID, @QueryParam("action") String action) {
        return getDB().query("""
            select id_at_app_module, dict_data_auth
            from %s
            where id_at_auth_role = ?
                and v_alias_at_app_module_action = ?
            """.formatted(roleActionController.getSchemaDotTable()), roleID, action);
    }

    @GET
    @Path("/availableRolesByAction/{moduleID}")
    @Operation(summary = "在模块id基础上，根据功能名查询有权限的角色")
    public List<Map<String, Object>> availableRolesByAction(@PathParam("moduleID") String moduleID, @QueryParam("action") String action) {
        return getDB().query("""
            select id_at_auth_role, dict_data_auth
            from %s
            where id_at_app_module = ?
                and v_alias_at_app_module_action = ?
            """.formatted(roleActionController.getSchemaDotTable()), moduleID, action);
    }

    @GET
    @Path("/assign/{roleID}/to/{userID}")
    public String assign(@PathParam("roleID") String roleID, @PathParam("userID") String userID) {
        try {
            return this.createChild(roleID, getUserRoleChild().getChildAlias(), Map.of(
                "id_at_auth_user", userID
            ));
        } catch (Exception ignored) {
            return "";
        }
    }

    @GET
    @Path("/revoke/{roleID}/to/{userID}")
    public Integer revoke(@PathParam("roleID") String roleID, @PathParam("userID") String userID) {
        PageResult<Map> result = userRoleController.query(Map.of(
            "id_at_auth_user", userID,
            "id_at_auth_role", roleID
        ));

        if (result.getSize() == 1) {
            String id = (String) result.getList().get(0).get("id");
            return this.deleteChild(roleID, getUserRoleChild().getChildAlias(), id);
        }

        return 1;
    }

    @Override
    public ModuleController getModuleController() {
        return moduleController;
    }

    @Override
    public ModuleConfig getModuleConfig() {
        return ModuleConfig.ofName("角色管理")
            .setTable("auth_role")
            .setUrl("platform/role/index")
            .setAlias(MODULE_ALIAS)
            .addAction(new ModuleAction("assign", "分配", ModuleAction.TypeLike.UPDATE))
            .addAction(new ModuleAction("revoke", "撤回", ModuleAction.TypeLike.UPDATE));
    }

    @Override
    public void beforeDelete(String id) {
        super.beforeDelete(id);

        PageResult result = userRoleController.query(Map.of(
            "id_at_auth_role", id
        ));

        if (result.getSize() > 0) {
            throw new MyException("该角色已经给相关人员，不允许删除");
        }

    }
}
