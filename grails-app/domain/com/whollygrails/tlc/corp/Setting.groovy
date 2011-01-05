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

class Setting {

    static traceInsertCode = new AtomicLong()
    static traceUpdateCode = new AtomicLong()
    static traceDeleteCode = new AtomicLong()

    static belongsTo = [company: Company]

    String code
    String dataType
    Integer dataScale
    String value
    Long securityCode = 0
    Date dateCreated
    Date lastUpdated

    static mapping = {
        columns {
            company lazy: true
        }
    }

    static constraints = {
        code(blank: false, size: 1..100, unique: 'company')
        dataType(blank: false, inList: ['string', 'integer', 'decimal', 'date', 'boolean'])
        dataScale(nullable: true, range: 1..10, validator: {val, obj ->
            switch (obj.dataType) {
                case 'date':
                    if (val != 1 && val != 2) return 'date.error'
                    break

                case 'decimal':
                    if (!val) return 'decimal.error'
                    break

                default:
                    if (val) return 'other.error'
                    break
            }

            return true
        })
        value(blank: false, size: 1..100, validator: {val, obj ->
            if (val != null) {
                def result = UtilService.stringOf(obj.dataType, obj.dataScale, UtilService.valueOf(obj.dataType, obj.dataScale, val))
                if (result == null) return 'validation.error'
                obj.value = result
            }

            return true
        })
        securityCode(validator: {val, obj ->
            obj.securityCode = obj.company.securityCode
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
        return code
    }
}
