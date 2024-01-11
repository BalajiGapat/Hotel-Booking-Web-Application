package com.app.controller;

import com.app.exception.RoleAlreadyExistException;
import com.app.model.Role;
import com.app.model.User;
import com.app.service.IRoleService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.FOUND;


@RestController
@RequestMapping("/roles")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@RequiredArgsConstructor
public class RoleController 
{
    private final IRoleService roleService;

    @GetMapping("/all-roles") // tested
    public ResponseEntity<List<Role>> getAllRoles()
    {
        return new ResponseEntity<>(roleService.getRoles(), FOUND);
    }

    
    
    @PostMapping("/create-new-role") // tested
    public ResponseEntity<String> createRole(@RequestBody Role theRole)
    {
        try
        {
            roleService.createRole(theRole);
            return ResponseEntity.ok("New role created successfully!");
        }
        catch(RoleAlreadyExistException re)
        {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(re.getMessage());
        }
    }
    
    
    
    
    @DeleteMapping("/delete/{roleId}") // tested
    public void deleteRole(@PathVariable("roleId") Long roleId)
    {
        roleService.deleteRole(roleId);
    }
    
    @PostMapping("/remove-all-users-from-role/{roleId}") // tested
    public Role removeAllUsersFromRole(@PathVariable("roleId") Long roleId)
    {
        return roleService.removeAllUsersFromRole(roleId);
    }

    
    
    @PostMapping("/remove-user-from-role") // tested
    public User removeUserFromRole( @RequestParam("userId") Long userId, @RequestParam("roleId") Long roleId )
    {
        return roleService.removeUserFromRole(userId, roleId);
    }
    
    
    
    @PostMapping("/assign-user-to-role") // tested
    public User assignUserToRole(@RequestParam("userId") Long userId, @RequestParam("roleId") Long roleId)
    {
        return roleService.assignRoleToUser(userId, roleId);
    }
    
    
}
