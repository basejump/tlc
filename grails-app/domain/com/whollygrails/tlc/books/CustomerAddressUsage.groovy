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

import com.whollygrails.tlc.sys.SystemCustomerAddressType
import com.whollygrails.tlc.sys.UtilService
import java.util.concurrent.atomic.AtomicLong

class CustomerAddressUsage {

    static traceInsertCode = new AtomicLong()
    static traceUpdateCode = new AtomicLong()
    static traceDeleteCode = new AtomicLong()

    static belongsTo = [customer: Customer, address: CustomerAddress, type: SystemCustomerAddressType]

    Long securityCode = 0
    Date dateCreated
    Date lastUpdated

    static mapping = {
        columns {
            customer lazy: true
            address lazy: true
            type lazy: true
        }
    }

    static constraints = {
        customer(validator: {val, obj ->
            return (val?.id == obj.address?.customer?.id)
        })
        securityCode(validator: {val, obj ->
            obj.securityCode = obj.customer.securityCode
            return true
        })
    }

    def afterInsert = {
        return UtilService.trace('insert', this)
    }

    def afterUpdate = {
        return UtilService.trace('update', this)
    }

    def afterDelete = {
        return UtilService.trace('delete', this)
    }

    public String toString() {
        return "${id}"
    }
}
