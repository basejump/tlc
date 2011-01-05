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
package com.whollygrails.tlc.corp

import com.whollygrails.tlc.sys.UtilService
import java.util.concurrent.atomic.AtomicLong

class QueuedTaskResult {

    static traceInsertCode = new AtomicLong()
    static traceUpdateCode = new AtomicLong()
    static traceDeleteCode = new AtomicLong()

    static belongsTo = [queued: QueuedTask, result: TaskResult]

    String value
    Long securityCode = 0
    Date dateCreated
    Date lastUpdated

    static mapping = {
        columns {
            queued lazy: true
            result lazy: true
        }
    }

    static constraints = {
        result(validator: {val, obj ->
            return (val.task.id == obj.queued.task.id) ?: 'validation.error'
        })
        value(nullable: true, size: 1..200, validator: {val, obj ->
            if (val != null) {
                def rslt = UtilService.stringOf(obj.result.dataType, obj.result.dataScale, UtilService.valueOf(obj.result.dataType, obj.result.dataScale, val))
                if (rslt == null) return 'validation.error'
                obj.value = rslt
            }

            return true
        })
        securityCode(validator: {val, obj ->
            obj.securityCode = obj.queued.securityCode
            return true
        })
    }

    def afterInsert = {
        UtilService.trace('insert', this)
    }

    def afterUpdate = {
        UtilService.trace('update', this)
    }

    def afterDelete = {
        UtilService.trace('delete', this)
    }

    public String toString() {
        return "${id}"
    }
}
