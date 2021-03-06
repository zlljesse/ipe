package com.ipe.module.core.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ipe.common.dao.BaseDao;
import com.ipe.common.dao.SpringJdbcDao;
import com.ipe.common.service.BaseService;
import com.ipe.module.core.dao.AuthorityDao;
import com.ipe.module.core.dao.ResourceDao;
import com.ipe.module.core.dao.RoleDao;
import com.ipe.module.core.dao.UserDao;
import com.ipe.module.core.dao.UserRoleDao;
import com.ipe.module.core.entity.Authority;
import com.ipe.module.core.entity.Resource;
import com.ipe.module.core.entity.Role;
import com.ipe.module.core.entity.User;
import com.ipe.module.core.entity.UserRole;

/**
 * Created with IntelliJ IDEA.
 * User: tangdu
 * Date: 13-9-14
 * Time: 下午11:00
 * To change this template use File | Settings | File Templates.
 */
@Service
@Transactional(readOnly = true)
public class RoleService extends BaseService<Role, String> {
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private UserRoleDao userRoleDao;
    @Autowired
    private AuthorityDao authorityDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private SpringJdbcDao springJdbcDao;

    @Override
    public BaseDao<Role, String> getBaseDao() {
        return roleDao;
    }

    @Transactional
    public void saveUserRole(String[] urids, String userId) {
        //先删除
        roleDao.delUserRole(userId);
        //新增
        User user = userDao.get(userId);
        for (String roleId : urids) {
            if (StringUtils.isNotBlank(roleId)) {
                Role role = roleDao.get(roleId);
                UserRole userRole = new UserRole();
                userRole.setRole(role);
                userRole.setUser(user);
                userRoleDao.save(userRole);
            }
        }
    }

    //配置权限
    @Transactional(readOnly = false)
    public void saveAuthority(String[] ids, String roleId) {
        //先删除
        authorityDao.delRoleAuthority(roleId);
        //新增
        Role role = roleDao.get(roleId);
        if (ids != null) {
            for (String id : ids) {
                if (StringUtils.isNotBlank(id)) {
                    Resource resource = resourceDao.get(id);
                    Authority authority = new Authority();
                    authority.setResource(resource);
                    authority.setRole(role);
                    authorityDao.save(authority);
                }
            }
        }
    }

    //巳有权限
    public Resource getAuthoritys(final String roleId) {
        //查询所有资源-树结构-带复选框
    	Resource root = resourceService.getChecdkboxTreeResources();
        //查询角色资源
        Set<Resource> mylist = authorityDao.getRoleByAuthority(roleId);
        if (mylist != null && !mylist.isEmpty()) {
            eachAuthoritys(mylist, root);
        }
        return root;
    }

    public void eachAuthoritys(Set<Resource> mylist, Resource root) {
    	for (Resource r2 : mylist) {
            if (r2.getId().equals(root.getId())) {
            	root.setChecked(true);
                break;
            }
        }
    	if(root.getRows()!=null && !root.getRows().isEmpty()){
    		for(Resource r1:root.getRows()){
    			eachAuthoritys(mylist,r1);
    		}
    	}
    }
    
    /**
     * 得到用户所有角色
     * @param userId
     * @return
     */
    public List<Role> getUserRoles(String userId){
    	String hql="select t.role from UserRole t where t.user.id=?";
    	return this.roleDao.list(hql, userId);
    }
    
    /**
     * 得到用户所有资源权限
     * @param userId
     * @return
     */
    @SuppressWarnings("unchecked")
	public List<String> getUserAuthorits(String userId){
    	String sql="select t5.resource_url from t_cor_user t1 join "+
			"t_cor_user_role t2 on t1.id_=t2.user_id "+
			"join t_cor_role t3 on t3.id_=t2.role_id "+
			"join t_cor_authority t4 on t4.role_id=t3.id_ "+
			"join t_cor_resource t5 on t5.id_=t4.resource_id "+
			"where t1.id_=? and t5.resource_url is not null  and t5.resource_url <>''";
    	List<Object> conditions=new ArrayList<Object>();
    	conditions.add(userId);
    	return (List<String>) this.springJdbcDao.queryList(sql, conditions,String.class);
    }
}
