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
package com.whollygrails.tlc.sys

import com.whollygrails.tlc.corp.Company

class SystemTracingController {

    // Injected services
    def utilService

    // Security settings
    def activities = [default: 'sysadmin']

    // List of actions with specific request types
    static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST']

    def index = { redirect(action: list, params: params) }

    def list = {
        params.max = (params.max && params.max.toInteger() > 0) ? Math.min(params.max.toInteger(), utilService.setting('pagination.max', 50)) : utilService.setting('pagination.default', 20)
        params.sort = ['domainName', 'insertRetentionDays', 'updateRetentionDays', 'deleteRetentionDays', 'systemOnly'].contains(params.sort) ? params.sort : 'domainName'
        def systemTracingInstanceList = SystemTracing.selectList()
        for (tracing in systemTracingInstanceList) {
            tracing.insertDecode = decodeSetting(tracing.insertSecurityCode)
            tracing.updateDecode = decodeSetting(tracing.updateSecurityCode)
            tracing.deleteDecode = decodeSetting(tracing.deleteSecurityCode)
        }

        [systemTracingInstanceList: systemTracingInstanceList, systemTracingInstanceTotal: SystemTracing.selectCount()]
    }

    def show = {
        def systemTracingInstance = SystemTracing.get(params.id)
        if (!systemTracingInstance) {
            flash.message = 'systemTracing.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Tracing not found with id ${params.id}"
            redirect(action: list)
        } else {
            systemTracingInstance.insertDecode = decodeSetting(systemTracingInstance.insertSecurityCode)
            systemTracingInstance.updateDecode = decodeSetting(systemTracingInstance.updateSecurityCode)
            systemTracingInstance.deleteDecode = decodeSetting(systemTracingInstance.deleteSecurityCode)
            return [systemTracingInstance: systemTracingInstance]
        }
    }

    def delete = {
        def systemTracingInstance = SystemTracing.get(params.id)
        if (systemTracingInstance) {
            try {
                systemTracingInstance.delete(flush: true)
                SecurityService.setTracing(systemTracingInstance.domainName, UtilService.TRACE_NONE, UtilService.TRACE_NONE, UtilService.TRACE_NONE)
                flash.message = 'systemTracing.deleted'
                flash.args = [systemTracingInstance.toString()]
                flash.defaultMessage = "Tracing ${systemTracingInstance.toString()} deleted"
                redirect(action: list)
            } catch (Exception e) {
                flash.message = 'systemTracing.not.deleted'
                flash.args = [systemTracingInstance.toString(), e.class.simpleName]
                flash.defaultMessage = "Tracing ${systemTracingInstance.toString()} could not be deleted (${e.class.simpleName})"
                redirect(action: show, id: params.id)
            }
        } else {
            flash.message = 'systemTracing.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Tracing not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def edit = {
        def systemTracingInstance = SystemTracing.get(params.id)
        if (!systemTracingInstance) {
            flash.message = 'systemTracing.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Tracing not found with id ${params.id}"
            redirect(action: list)
        } else {
            return [systemTracingInstance: systemTracingInstance, selectionList: createSelectionList(systemTracingInstance.systemOnly)]
        }
    }

    def update = {
        def systemTracingInstance = SystemTracing.get(params.id)
        if (systemTracingInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (systemTracingInstance.version > version) {
                    systemTracingInstance.errors.rejectValue('version', 'systemTracing.optimistic.locking.failure', 'Another user has updated this Tracing while you were editing')
                    render(view: 'edit', model: [systemTracingInstance: systemTracingInstance, selectionList: createSelectionList(systemTracingInstance.systemOnly)])
                    return
                }
            }

            def oldDomainName = systemTracingInstance.domainName
            systemTracingInstance.properties['domainName', 'insertSecurityCode', 'updateSecurityCode', 'deleteSecurityCode', 'insertRetentionDays', 'updateRetentionDays', 'deleteRetentionDays', 'systemOnly'] = params
            if (!systemTracingInstance.hasErrors() && systemTracingInstance.saveThis()) {
                if (oldDomainName != systemTracingInstance.domainName) SecurityService.setTracing(systemTracingInstance.domainName, UtilService.TRACE_NONE, UtilService.TRACE_NONE, UtilService.TRACE_NONE)
                SecurityService.setTracing(systemTracingInstance.domainName, systemTracingInstance.insertSecurityCode, systemTracingInstance.updateSecurityCode, systemTracingInstance.deleteSecurityCode)
                flash.message = 'systemTracing.updated'
                flash.args = [systemTracingInstance.toString()]
                flash.defaultMessage = "Tracing ${systemTracingInstance.toString()} updated"
                redirect(action: show, id: systemTracingInstance.id)
            } else {
                render(view: 'edit', model: [systemTracingInstance: systemTracingInstance, selectionList: createSelectionList(systemTracingInstance.systemOnly)])
            }
        } else {
            flash.message = 'systemTracing.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Tracing not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def create = {
        return [systemTracingInstance: new SystemTracing(), selectionList: createSelectionList(false)]
    }

    def save = {
        def systemTracingInstance = new SystemTracing()
        systemTracingInstance.properties['domainName', 'insertSecurityCode', 'updateSecurityCode', 'deleteSecurityCode', 'insertRetentionDays', 'updateRetentionDays', 'deleteRetentionDays', 'systemOnly'] = params
        if (!systemTracingInstance.hasErrors() && systemTracingInstance.saveThis()) {
            SecurityService.setTracing(systemTracingInstance.domainName, systemTracingInstance.insertSecurityCode, systemTracingInstance.updateSecurityCode, systemTracingInstance.deleteSecurityCode)
            flash.message = 'systemTracing.created'
            flash.args = [systemTracingInstance.toString()]
            flash.defaultMessage = "Tracing ${systemTracingInstance.toString()} created"
            redirect(action: show, id: systemTracingInstance.id)
        } else {
            render(view: 'create', model: [systemTracingInstance: systemTracingInstance, selectionList: createSelectionList(systemTracingInstance.systemOnly)])
        }
    }

    private createSelectionList(systemOnly) {
        def selectionList = []
        selectionList << [id: UtilService.TRACE_NONE, name: message(code: 'systemTracing.traceNone', default: '-- No Tracing --')]
        selectionList << [id: UtilService.TRACE_ALL, name: message(code: 'systemTracing.traceAll', default: '-- Trace All --')]
        if (!systemOnly) {
            Company.list([sort: 'name']).each {
                selectionList << [id: it.securityCode, name: it.name]
            }
        }

        return selectionList
    }

    private decodeSetting(code) {
        if (code == UtilService.TRACE_NONE) return message(code: 'systemTracing.traceNone', default: '-- No Tracing --')
        if (code == UtilService.TRACE_ALL) return message(code: 'systemTracing.traceAll', default: '-- Trace All --')
        def company = Company.findBySecurityCode(code)
        if (company) return company.name
        return message(code: 'systemTracing.no.company', args: [code], default: "Unknown company security code ${code}")
    }
}