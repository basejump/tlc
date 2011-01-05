/*
 *  Copyright 2010 Wholly Grails.
 *
 *  This file is part of the Three Ledger Core (TLC) software
 *  from Wholly Grails.
 *
 *  TLC is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  TLC is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TLC.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.whollygrails.tlc.books

import com.whollygrails.tlc.corp.CompanyUser
import com.whollygrails.tlc.sys.SystemUser

class AccessGroupController {

    // Injected services
    def utilService

    // Security settings
    def activities = [default: 'coadmin']

    // List of actions with specific request types
    static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST', adjust: 'POST', process: 'POST']

    def index = { redirect(action: list, params: params) }

    def list = {
        params.max = (params.max && params.max.toInteger() > 0) ? Math.min(params.max.toInteger(), utilService.setting('pagination.max', 50)) : utilService.setting('pagination.default', 20)
        params.sort = ['code', 'name', 'element1', 'element2', 'element3', 'element4', 'element5', 'element6', 'element7', 'element8', 'customers', 'suppliers'].contains(params.sort) ? params.sort : 'code'
        [accessGroupInstanceList: AccessGroup.selectList(company: utilService.currentCompany()), accessGroupInstanceTotal: AccessGroup.selectCount(), elementList: makeElementList()]
    }

    def show = {
        def accessGroupInstance = AccessGroup.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (!accessGroupInstance) {
            flash.message = 'accessGroup.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Access Group not found with id ${params.id}"
            redirect(action: list)
        } else {
            return [accessGroupInstance: accessGroupInstance, elementList: makeElementList()]
        }
    }

    def delete = {
        def accessGroupInstance = AccessGroup.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (accessGroupInstance) {
            try {
                def valid = true
                AccessGroup.withTransaction {status ->

                    // Need to avoid concurrent modification exceptions
                    def temp = []
                    accessGroupInstance.users.each {
                        temp << it
                    }

                    temp.each {
                        accessGroupInstance.removeFromUsers(it)
                    }

                    if (accessGroupInstance.save()) {   // With deep validation
                        accessGroupInstance.delete(flush: true)
                    } else {
                        status.setRollbackOnly()
                        accessGroupInstance.errorMessage(code: 'accessGroup.remove', default: 'Unable to remove the members from the group')
                        valid = false
                    }
                }

                if (valid) {
                    utilService.cacheService.resetThis('accessGroup', accessGroupInstance.securityCode, accessGroupInstance.code)
                    utilService.cacheService.resetAll('userCustomer', accessGroupInstance.securityCode)
                    utilService.cacheService.resetAll('userSupplier', accessGroupInstance.securityCode)
                    utilService.cacheService.resetAll('userAccessGroup', accessGroupInstance.securityCode)
                    utilService.cacheService.resetAll('userAccount', accessGroupInstance.securityCode)
                    flash.message = 'accessGroup.deleted'
                    flash.args = [accessGroupInstance.toString()]
                    flash.defaultMessage = "Access Group ${accessGroupInstance.toString()} deleted"
                    redirect(action: list)
                } else {
                    redirect(action: show, id: params.id)
                }
            } catch (Exception e) {
                flash.message = 'accessGroup.not.deleted'
                flash.args = [accessGroupInstance.toString(), e.class.simpleName]
                flash.defaultMessage = "Access Group ${accessGroupInstance.toString()} could not be deleted (${e.class.simpleName})"
                redirect(action: show, id: params.id)
            }
        } else {
            flash.message = 'accessGroup.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Access Group not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def edit = {
        def accessGroupInstance = AccessGroup.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (!accessGroupInstance) {
            flash.message = 'accessGroup.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Access Group not found with id ${params.id}"
            redirect(action: list)
        } else {
            return [accessGroupInstance: accessGroupInstance, elementList: makeElementList()]
        }
    }

    def update = {
        def accessGroupInstance = AccessGroup.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (accessGroupInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (accessGroupInstance.version > version) {
                    accessGroupInstance.errors.rejectValue('version', 'accessGroup.optimistic.locking.failure', 'Another user has updated this Access Group while you were editing')
                    render(view: 'edit', model: [accessGroupInstance: accessGroupInstance, elementList: makeElementList()])
                    return
                }
            }

            def oldCode = accessGroupInstance.code
            accessGroupInstance.properties['code', 'name', 'element1', 'element2', 'element3', 'element4', 'element5', 'element6', 'element7', 'element8', 'customers', 'suppliers'] = params
            if (!accessGroupInstance.hasErrors() && accessGroupInstance.saveThis()) {
                utilService.cacheService.resetThis('accessGroup', accessGroupInstance.securityCode, oldCode)
                utilService.cacheService.resetAll('userCustomer', accessGroupInstance.securityCode)
                utilService.cacheService.resetAll('userSupplier', accessGroupInstance.securityCode)
                utilService.cacheService.resetAll('userAccessGroup', accessGroupInstance.securityCode)
                utilService.cacheService.resetAll('userAccount', accessGroupInstance.securityCode)
                flash.message = 'accessGroup.updated'
                flash.args = [accessGroupInstance.toString()]
                flash.defaultMessage = "Access Group ${accessGroupInstance.toString()} updated"
                redirect(action: show, id: accessGroupInstance.id)
            } else {
                render(view: 'edit', model: [accessGroupInstance: accessGroupInstance, elementList: makeElementList()])
            }
        } else {
            flash.message = 'accessGroup.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Access Group not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def create = {
        def accessGroupInstance = new AccessGroup()
        accessGroupInstance.company = utilService.currentCompany()   // Ensure correct company
        return [accessGroupInstance: accessGroupInstance, elementList: makeElementList()]
    }

    def save = {
        def accessGroupInstance = new AccessGroup()
        accessGroupInstance.properties['code', 'name', 'element1', 'element2', 'element3', 'element4', 'element5', 'element6', 'element7', 'element8', 'customers', 'suppliers'] = params
        accessGroupInstance.company = utilService.currentCompany()   // Ensure correct company
        if (!accessGroupInstance.hasErrors() && accessGroupInstance.saveThis()) {
            flash.message = 'accessGroup.created'
            flash.args = [accessGroupInstance.toString()]
            flash.defaultMessage = "Access Group ${accessGroupInstance.toString()} created"
            redirect(action: show, id: accessGroupInstance.id)
        } else {
            render(view: 'create', model: [accessGroupInstance: accessGroupInstance, elementList: makeElementList()])
        }
    }

    def display = {
        params.max = (params.max && params.max.toInteger() > 0) ? Math.min(params.max.toInteger(), utilService.setting('pagination.max', 50)) : utilService.setting('pagination.default', 20)
        params.sort = ['code', 'name'].contains(params.sort) ? params.sort : 'code'
        def ddSource = utilService.source('companyUser.display')
        [accessGroupInstanceList: AccessGroup.selectList(), accessGroupInstanceTotal: AccessGroup.selectCount(), ddSource: ddSource]
    }

    def edits = {
        def ddSource = utilService.reSource('companyUser.display', [origin: 'display'])
        [allGroups: AccessGroup.findAllByCompany(utilService.currentCompany()), userGroups: AccessGroup.selectList(action: 'display'), ddSource: ddSource]
    }

    def adjust = {
        def ddSource = utilService.reSource('companyUser.display', [origin: 'display'])
        def userGroups = AccessGroup.selectList(action: 'display')
        def groups = []
        if (params.linkages) groups = params.linkages instanceof String ? [params.linkages.toLong()] : params.linkages*.toLong() as List
        def modified = false

        // Work through the existing groups
        for (group in userGroups) {

            // If the existing group is not in the new groups, remove it
            if (!groups.contains(group.id)) {
                ddSource.removeFromAccessGroups(group)
                modified = true
            }
        }

        // Work through the new groups
        for (group in groups) {
            def found = false

            // Check if the new group is in the exiting groups
            for (g in userGroups) {
                if (g.id == group) {
                    found = true
                    break
                }
            }

            // Add the new group if it wasn't in the existing groups
            if (!found) {
                ddSource.addToAccessGroups(AccessGroup.get(group))
                modified = true
            }
        }

        if (modified) {
            if (ddSource.save(flush: true)) {      // With deep validation
                utilService.cacheService.resetThis('userAccessGroup', ddSource.securityCode, ddSource.user.id.toString())
                utilService.cacheService.resetThis('userAccount', ddSource.securityCode, ddSource.user.id.toString())
                utilService.cacheService.resetThis('userCustomer', ddSource.securityCode, ddSource.user.id.toString())
                utilService.cacheService.resetThis('userSupplier', ddSource.securityCode, ddSource.user.id.toString())
                flash.message = 'accessGroup.groups.changed'
                flash.defaultMessage = 'User access groups updated'
            } else {
                flash.message = 'accessGroup.groups.failed'
                flash.defaultMessage = 'Error updating the access groups'
            }
        } else {
            flash.message = 'accessGroup.groups.unchanged'
            flash.defaultMessage = 'No access groups were changed'
        }

        redirect(action: display)
    }

    def members = {
        params.max = (params.max && params.max.toInteger() > 0) ? Math.min(params.max.toInteger(), utilService.setting('pagination.max', 50)) : utilService.setting('pagination.default', 20)
        params.sort = ['loginId', 'name'].contains(params.sort) ? params.sort : 'name'
        params.order = ['asc', 'desc'].contains(params.order) ? params.order : 'asc'
        def ddSource = utilService.source('accessGroup.list')
        def memberList = SystemUser.findAll("from SystemUser as su where su.id in (select cu.user.id from CompanyUser as cu join cu.accessGroups as ag where ag = ?) order by su.${params.sort} ${params.order}", [ddSource])
        def memberTotal = SystemUser.executeQuery('select count(*) from SystemUser as su where su.id in (select cu.user.id from CompanyUser as cu join cu.accessGroups as ag where ag = ?)', [ddSource])[0]
        [memberList: memberList, memberTotal: memberTotal, ddSource: ddSource]
    }

    def membership = {
        def ddSource = utilService.reSource('accessGroup.list', [origin: 'members'])
        def memberList = SystemUser.findAll('from SystemUser as su where su.id in (select cu.user.id from CompanyUser as cu join cu.accessGroups as ag where ag = ?) order by su.name', [ddSource])
        def userList = SystemUser.findAll('from SystemUser as su where su.id in (select cu.user.id from CompanyUser as cu where cu.company = ?) order by su.name', [utilService.currentCompany()])
        [userList: userList, memberList: memberList, ddSource: ddSource]
    }

    def process = {
        def ddSource = utilService.reSource('accessGroup.list', [origin: 'members'])
        def memberList = SystemUser.findAll('from SystemUser as su where su.id in (select cu.user.id from CompanyUser as cu join cu.accessGroups as ag where ag = ?) order by su.name', [ddSource])
        def members = []
        if (params.linkages) members = params.linkages instanceof String ? [params.linkages.toLong()] : params.linkages*.toLong() as List
        def modified = []

        // Work through the existing membership
        for (member in memberList) {

            // If the existing member is not in the new membership, remove them
            if (!members.contains(member.id)) {
                ddSource.removeFromUsers(CompanyUser.findByCompanyAndUser(utilService.currentCompany(), member, [cache: true]))
                modified << member
            }
        }

        // Work through the new membership
        for (member in members) {
            def found = false

            // Check if the new group is in the exiting groups
            for (m in memberList) {
                if (m.id == member) {
                    found = true
                    break
                }
            }

            // Add the new group if it wasn't in the existing groups
            if (!found) {
                def user = SystemUser.get(member)
                if (user) {
                    ddSource.addToUsers(CompanyUser.findByCompanyAndUser(utilService.currentCompany(), user, [cache: true]))
                    modified << user
                }
            }
        }

        if (modified) {
            if (ddSource.save(flush: true)) {      // With deep validation
                modified.each {
                    utilService.cacheService.resetThis('userAccessGroup', ddSource.securityCode, it.id.toString())
                    utilService.cacheService.resetThis('userAccount', ddSource.securityCode, it.id.toString())
                    utilService.cacheService.resetThis('userCustomer', ddSource.securityCode, it.id.toString())
                    utilService.cacheService.resetThis('userSupplier', ddSource.securityCode, it.id.toString())
                }

                flash.message = 'accessGroup.members.changed'
                flash.defaultMessage = 'Access group membership updated'
            } else {
                flash.message = 'accessGroup.members.failed'
                flash.defaultMessage = 'Error updating the access group membership'
            }
        } else {
            flash.message = 'accessGroup.members.unchanged'
            flash.defaultMessage = 'No changes were made to the access group membership'
        }

        redirect(action: 'members')
    }

    private makeElementList() {
        def list = []
        for (int i = 0; i < 8; i++) {
            list << ''
        }

        CodeElement.findAllByCompany(utilService.currentCompany()).each {
            list[it.elementNumber - 1] = it.name
        }

        return list
    }
}