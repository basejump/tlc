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

import com.whollygrails.tlc.sys.SystemAddressFormat
import com.whollygrails.tlc.sys.SystemCountry
import com.whollygrails.tlc.sys.SystemSupplierAddressType

class SupplierAddressController {

    // Injected services
    def utilService
    def bookService
    def addressService

    // Security settings
    def activities = [default: 'apadmin']

    // List of actions with specific request types
    static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST', initializing: 'POST']

    def index = { redirect(action: list, params: params) }

    def list = {
        params.max = (params.max && params.max.toInteger() > 0) ? Math.min(params.max.toInteger(), utilService.setting('pagination.max', 50)) : utilService.setting('pagination.default', 20)
        params.sort = 'id'
        def ddSource = utilService.source('supplier.list')
        def supplierAddressInstanceList = []
        def supplierAddressInstanceTotal = 0
        def supplierAddressLines = []
        if (bookService.hasSupplierAccess(ddSource) && !addressService.getDummyAddress(ddSource)) {
            supplierAddressInstanceList = SupplierAddress.selectList(securityCode: utilService.currentCompany().securityCode)
            supplierAddressInstanceTotal = SupplierAddress.selectCount()
            def sendingCountry = utilService.currentCompany().country
            for (address in supplierAddressInstanceList) supplierAddressLines << addressService.formatAddress(address, null, null, sendingCountry)
        }

        [supplierAddressInstanceList: supplierAddressInstanceList, supplierAddressInstanceTotal: supplierAddressInstanceTotal, ddSource: ddSource, supplierAddressLines: supplierAddressLines]
    }

    def show = {
        def supplierAddressInstance = SupplierAddress.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (!bookService.hasSupplierAccess(supplierAddressInstance?.supplier)) {
            flash.message = 'supplierAddress.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Supplier Address not found with id ${params.id}"
            redirect(action: list)
        } else {
            return [supplierAddressInstance: supplierAddressInstance,
                    supplierAddressLines: addressService.getAsLineMaps(supplierAddressInstance, null, null, utilService.currentCompany().country)]
        }
    }

    def delete = {
        def supplierAddressInstance = SupplierAddress.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (bookService.hasSupplierAccess(supplierAddressInstance?.supplier)) {
            try {
                if (supplierAddressInstance.addressUsages) {
                    flash.message = 'supplierAddress.bad.delete'
                    flash.args = [supplierAddressInstance.toString()]
                    flash.defaultMessage = "You cannot delete Supplier Address ${supplierAddressInstance.toString()} until you have re-assigned its usages"
                    redirect(action: show, id: params.id)
                } else {
                    supplierAddressInstance.delete(flush: true)
                    flash.message = 'supplierAddress.deleted'
                    flash.args = [supplierAddressInstance.toString()]
                    flash.defaultMessage = "Supplier Address ${supplierAddressInstance.toString()} deleted"
                    redirect(action: list)
                }
            } catch (Exception e) {
                flash.message = 'supplierAddress.not.deleted'
                flash.args = [supplierAddressInstance.toString(), e.class.simpleName]
                flash.defaultMessage = "Supplier Address ${supplierAddressInstance.toString()} could not be deleted (${e.class.simpleName})"
                redirect(action: show, id: params.id)
            }
        } else {
            flash.message = 'supplierAddress.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Supplier Address not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def edit = {
        def supplierAddressInstance = SupplierAddress.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (!bookService.hasSupplierAccess(supplierAddressInstance?.supplier)) {
            flash.message = 'supplierAddress.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Supplier Address not found with id ${params.id}"
            redirect(action: list)
        } else {
            return [supplierAddressInstance: supplierAddressInstance,
                    supplierAddressLines: addressService.getAsLineMaps(supplierAddressInstance),
                    transferList: createTransferList(supplierAddressInstance)]
        }
    }

    def update = {
        def ddSource = utilService.reSource('supplier.list')
        if (params.modified) {
            def modified = processModification(ddSource, params)
            if (modified) {
                render(view: 'edit', model: [supplierAddressInstance: loadTransfers(modified, params),
                        supplierAddressLines: addressService.getAsLineMaps(modified),
                        transferList: createTransferList(modified)])
            } else {
                redirect(action: list)
            }
        } else {
            def supplierAddressInstance = SupplierAddress.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
            if (bookService.hasSupplierAccess(supplierAddressInstance?.supplier)) {
                if (params.version) {
                    def version = params.version.toLong()
                    if (supplierAddressInstance.version > version) {
                        supplierAddressInstance.errors.rejectValue('version', 'supplierAddress.optimistic.locking.failure', 'Another user has updated this Supplier Address while you were editing')
                        render(view: 'edit', model: [supplierAddressInstance: loadTransfers(supplierAddressInstance, params),
                                supplierAddressLines: addressService.getAsLineMaps(supplierAddressInstance),
                                transferList: createTransferList(supplierAddressInstance)])
                        return
                    }
                }

                supplierAddressInstance.properties['country', 'format', 'location1', 'location2', 'location3', 'metro1', 'metro2', 'area1', 'area2', 'encoding'] = params
                def valid = addressService.validate(loadTransfers(supplierAddressInstance, params))
                if (valid) {
                    def usage
                    SupplierAddress.withTransaction {status ->
                        for (trf in supplierAddressInstance.usageTransfers) {
                            usage = SupplierAddressUsage.findBySupplierAndType(supplierAddressInstance.supplier, trf)
                            if (usage && usage.address.id != supplierAddressInstance.id) usage.delete()
                            supplierAddressInstance.addToAddressUsages(new SupplierAddressUsage(supplier: supplierAddressInstance.supplier, type: trf))
                        }

                        if (!supplierAddressInstance.save(flush: true)) {
                            status.setRollbackOnly()
                            valid = false
                        }
                    }
                }

                if (valid) {
                    flash.message = 'supplierAddress.updated'
                    flash.args = [supplierAddressInstance.toString()]
                    flash.defaultMessage = "Supplier Address ${supplierAddressInstance.toString()} updated"
                    redirect(action: show, id: supplierAddressInstance.id)
                } else {
                    render(view: 'edit', model: [supplierAddressInstance: supplierAddressInstance,
                            supplierAddressLines: addressService.getAsLineMaps(supplierAddressInstance),
                            transferList: createTransferList(supplierAddressInstance)])
                }
            } else {
                flash.message = 'supplierAddress.not.found'
                flash.args = [params.id]
                flash.defaultMessage = "Supplier Address not found with id ${params.id}"
                redirect(action: list)
            }
        }
    }

    def create = {
        def ddSource = utilService.reSource('supplier.list')
        if (bookService.hasSupplierAccess(ddSource)) {
            def supplierAddressInstance = addressService.getDummyAddress(ddSource)
            if (!supplierAddressInstance) {
                supplierAddressInstance = new SupplierAddress()
                supplierAddressInstance.supplier = ddSource   // Ensure correct parent
                supplierAddressInstance.country = ddSource.country
                supplierAddressInstance.format = ddSource.country.addressFormat
            }

            return [supplierAddressInstance: supplierAddressInstance,
                    supplierAddressLines: addressService.getAsLineMaps(supplierAddressInstance),
                    transferList: createTransferList(supplierAddressInstance)]
        } else {
            redirect(action: list)
        }
    }

    def save = {
        def ddSource = utilService.reSource('supplier.list')
        if (params.modified) {
            def modified = processModification(ddSource, params)
            if (modified) {
                render(view: 'create', model: [supplierAddressInstance: loadTransfers(modified, params),
                        supplierAddressLines: addressService.getAsLineMaps(modified),
                        transferList: createTransferList(modified)])
            } else {
                redirect(action: list)
            }
        } else {
            if (bookService.hasSupplierAccess(ddSource)) {
                def supplierAddressInstance = addressService.getDummyAddress(ddSource)
                if (!supplierAddressInstance) {
                    supplierAddressInstance = new SupplierAddress()
                    supplierAddressInstance.supplier = ddSource   // Ensure correct parent
                }

                supplierAddressInstance.properties['country', 'format', 'location1', 'location2', 'location3', 'metro1', 'metro2', 'area1', 'area2', 'encoding'] = params
                def valid = addressService.validate(loadTransfers(supplierAddressInstance, params))
                if (valid) {
                    def usage
                    SupplierAddress.withTransaction {status ->
                        for (trf in supplierAddressInstance.usageTransfers) {
                            usage = SupplierAddressUsage.findBySupplierAndType(supplierAddressInstance.supplier, trf)
                            if (usage && usage.address.id != supplierAddressInstance.id) usage.delete()
                            supplierAddressInstance.addToAddressUsages(new SupplierAddressUsage(supplier: supplierAddressInstance.supplier, type: trf))
                        }

                        if (!supplierAddressInstance.save(flush: true)) {
                            status.setRollbackOnly()
                            valid = false
                        }
                    }
                }

                if (valid) {
                    flash.message = 'supplierAddress.created'
                    flash.args = [supplierAddressInstance.toString()]
                    flash.defaultMessage = "Supplier Address ${supplierAddressInstance.toString()} created"
                    redirect(action: show, id: supplierAddressInstance.id)
                } else {
                    render(view: 'create', model: [supplierAddressInstance: supplierAddressInstance,
                            supplierAddressLines: addressService.getAsLineMaps(supplierAddressInstance),
                            transferList: createTransferList(supplierAddressInstance)])
                }
            } else {
                redirect(action: list)
            }
        }
    }

    def initial = {
        def supplier = Supplier.get(params.id)
        if (bookService.hasSupplierAccess(supplier)) {
            def supplierAddressInstance = addressService.getDummyAddress(supplier)
            if (!supplierAddressInstance) {
                supplierAddressInstance = new SupplierAddress()
                supplierAddressInstance.supplier = supplier   // Ensure correct parent
                supplierAddressInstance.country = supplier.country
                supplierAddressInstance.format = supplier.country.addressFormat
            }

            return [supplierAddressInstance: supplierAddressInstance,
                    supplierAddressLines: addressService.getAsLineMaps(supplierAddressInstance),
                    transferList: createTransferList(supplierAddressInstance)]
        } else {
            redirect(controller: 'supplier', action: 'list')
        }
    }

    def initializing = {
        def supplier = Supplier.get(params.parent)
        if (params.modified) {
            def modified = processModification(supplier, params)
            if (modified) {
                render(view: 'initial', model: [supplierAddressInstance: modified, supplierAddressLines: addressService.getAsLineMaps(modified), transferList: null])
            } else {
                redirect(controller: 'supplier', action: 'list')
            }
        } else {
            if (bookService.hasSupplierAccess(supplier)) {
                def supplierAddressInstance = addressService.getDummyAddress(supplier)
                if (!supplierAddressInstance) {
                    supplierAddressInstance = new SupplierAddress()
                    supplierAddressInstance.supplier = supplier   // Ensure correct parent
                }

                supplierAddressInstance.properties['country', 'format', 'location1', 'location2', 'location3', 'metro1', 'metro2', 'area1', 'area2', 'encoding'] = params
                if (addressService.validate(supplierAddressInstance) && supplierAddressInstance.saveThis()) {
                    flash.message = 'supplierAddress.created'
                    flash.args = [supplierAddressInstance.toString()]
                    flash.defaultMessage = "Supplier Address ${supplierAddressInstance.toString()} created"
                    redirect(controller: 'supplier', action: 'show', id: supplier.id)
                } else {
                    render(view: 'initial', model: [supplierAddressInstance: supplierAddressInstance,
                            supplierAddressLines: addressService.getAsLineMaps(supplierAddressInstance),
                            transferList: null])
                }
            } else {
                redirect(controller: 'supplier', action: 'list')
            }
        }
    }

// --------------------------------------------- Support Methods ---------------------------------------------

    private processModification(supplier, params) {
        def supplierAddressInstance, temp
        if (params.id) {
            supplierAddressInstance = SupplierAddress.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        } else {
            supplierAddressInstance = new SupplierAddress()
            supplierAddressInstance.supplier = supplier
            supplierAddressInstance.country = supplier.country
            supplierAddressInstance.format = supplier.country.addressFormat
        }

        if (bookService.hasSupplierAccess(supplierAddressInstance?.supplier)) {
            supplierAddressInstance.properties['location1', 'location2', 'location3', 'metro1', 'metro2', 'area1', 'area2', 'encoding'] = params
            if (params.modified == 'country') {
                temp = SystemCountry.get(params.country.id)
                if (temp) {
                    supplierAddressInstance.country = temp
                    supplierAddressInstance.format = temp.addressFormat
                }
            } else {    // Format
                temp = SystemAddressFormat.get(params.format.id)
                if (temp) supplierAddressInstance.format = temp
            }
        } else {
            supplierAddressInstance = null
        }

        // Don't want Grails to automatically save it
        if (supplierAddressInstance?.id) supplierAddressInstance.discard()
        return supplierAddressInstance
    }

    private createTransferList(address) {
        def types = SystemSupplierAddressType.list()
        if (address.id) {
            def usages = SupplierAddressUsage.findAllByAddress(address)
            for (usage in usages) {
                for (int i = 0; i < types.size(); i++) {
                    if (types[i].id == usage.type.id) {
                        types.remove(i)
                        break
                    }
                }
            }
        }

        return types
    }

    private loadTransfers(address, params) {
        if (params.transfers) {
            address.usageTransfers = []
            def transfers = (params.transfers instanceof String) ? [params.transfers] : params.transfers
            for (transfer in transfers) address.usageTransfers << SystemSupplierAddressType.get(transfer)
        }

        return address
    }
}