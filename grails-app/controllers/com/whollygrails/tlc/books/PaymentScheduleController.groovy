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

import it.sauronsoftware.cron4j.Predictor

class PaymentScheduleController {

    // Injected services
    def utilService

    // Security settings
    def activities = [default: 'coadmin']

    // List of actions with specific request types
    static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST', testing: 'POST']

    def index = { redirect(action: list, params: params) }

    def list = {
        params.max = (params.max && params.max.toInteger() > 0) ? Math.min(params.max.toInteger(), utilService.setting('pagination.max', 50)) : utilService.setting('pagination.default', 20)
        params.sort = ['code', 'monthDayPattern', 'weekDayPattern'].contains(params.sort) ? params.sort : 'code'
        [paymentScheduleInstanceList: PaymentSchedule.selectList(company: utilService.currentCompany()), paymentScheduleInstanceTotal: PaymentSchedule.selectCount()]
    }

    def show = {
        def paymentScheduleInstance = PaymentSchedule.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (!paymentScheduleInstance) {
            flash.message = 'paymentSchedule.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Payment Schedule not found with id ${params.id}"
            redirect(action: list)
        } else {
            return [paymentScheduleInstance: paymentScheduleInstance]
        }
    }

    def delete = {
        def paymentScheduleInstance = PaymentSchedule.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (paymentScheduleInstance) {
            try {
                utilService.deleteWithMessages(paymentScheduleInstance, [prefix: 'paymentSchedule.name', code: paymentScheduleInstance.code])
                flash.message = 'paymentSchedule.deleted'
                flash.args = [paymentScheduleInstance.toString()]
                flash.defaultMessage = "Payment Schedule ${paymentScheduleInstance.toString()} deleted"
                redirect(action: list)
            } catch (Exception e) {
                flash.message = 'paymentSchedule.not.deleted'
                flash.args = [paymentScheduleInstance.toString(), e.class.simpleName]
                flash.defaultMessage = "Payment Schedule ${paymentScheduleInstance.toString()} could not be deleted (${e.class.simpleName})"
                redirect(action: show, id: params.id)
            }
        } else {
            flash.message = 'paymentSchedule.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Payment Schedule not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def edit = {
        def paymentScheduleInstance = PaymentSchedule.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (!paymentScheduleInstance) {
            flash.message = 'paymentSchedule.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Payment Schedule not found with id ${params.id}"
            redirect(action: list)
        } else {
            return [paymentScheduleInstance: paymentScheduleInstance]
        }
    }

    def update = {
        def paymentScheduleInstance = PaymentSchedule.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (paymentScheduleInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (paymentScheduleInstance.version > version) {
                    paymentScheduleInstance.errors.rejectValue('version', 'paymentSchedule.optimistic.locking.failure', 'Another user has updated this Payment Schedule while you were editing')
                    render(view: 'edit', model: [paymentScheduleInstance: paymentScheduleInstance])
                    return
                }
            }

            def oldCode = paymentScheduleInstance.code
            paymentScheduleInstance.properties['code', 'name', 'monthDayPattern', 'weekDayPattern'] = params
            if (utilService.saveWithMessages(paymentScheduleInstance, [prefix: 'paymentSchedule.name', code: paymentScheduleInstance.code, oldCode: oldCode, field: 'name'])) {
                flash.message = 'paymentSchedule.updated'
                flash.args = [paymentScheduleInstance.toString()]
                flash.defaultMessage = "Payment Schedule ${paymentScheduleInstance.toString()} updated"
                redirect(action: show, id: paymentScheduleInstance.id)
            } else {
                render(view: 'edit', model: [paymentScheduleInstance: paymentScheduleInstance])
            }
        } else {
            flash.message = 'paymentSchedule.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Payment Schedule not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def create = {
        def paymentScheduleInstance = new PaymentSchedule()
        paymentScheduleInstance.company = utilService.currentCompany()   // Ensure correct company
        return [paymentScheduleInstance: paymentScheduleInstance]
    }

    def save = {
        def paymentScheduleInstance = new PaymentSchedule()
        paymentScheduleInstance.properties['code', 'name', 'monthDayPattern', 'weekDayPattern'] = params
        paymentScheduleInstance.company = utilService.currentCompany()   // Ensure correct company
        if (utilService.saveWithMessages(paymentScheduleInstance, [prefix: 'paymentSchedule.name', code: paymentScheduleInstance.code, field: 'name'])) {
            flash.message = 'paymentSchedule.created'
            flash.args = [paymentScheduleInstance.toString()]
            flash.defaultMessage = "Payment Schedule ${paymentScheduleInstance.toString()} created"
            redirect(action: show, id: paymentScheduleInstance.id)
        } else {
            render(view: 'create', model: [paymentScheduleInstance: paymentScheduleInstance])
        }
    }

    def test = {
        [paymentScheduleInstance: new PaymentSchedule(), scheduleList: PaymentSchedule.findAllByCompany(utilService.currentCompany()), predictions: []]
    }

    def testing = {
        def paymentScheduleInstance = PaymentSchedule.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (!paymentScheduleInstance) {
            flash.message = 'paymentSchedule.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Payment Schedule not found with id ${params.id}"
            redirect(action: list)
        } else {
            def predictor = new Predictor(paymentScheduleInstance.pattern)
            def predictions = []
            for (int i = 0; i < 12; i++) predictions << predictor.nextMatchingDate()

            render(view: 'test', model: [paymentScheduleInstance: paymentScheduleInstance, scheduleList: PaymentSchedule.findAllByCompany(utilService.currentCompany()), predictions: predictions])
        }
    }
}