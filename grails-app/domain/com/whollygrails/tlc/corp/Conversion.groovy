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

class Conversion {

    static traceInsertCode = new AtomicLong()
    static traceUpdateCode = new AtomicLong()
    static traceDeleteCode = new AtomicLong()

    static belongsTo = [source: Unit, target: Unit]

    String code
    String name
    BigDecimal preAddition = 0.0
    BigDecimal multiplier
    BigDecimal postAddition = 0.0
    Long securityCode = 0
    Date dateCreated
    Date lastUpdated

    static mapping = {
        table 'company_conversion'
        columns {
            source column: 'source_unit', lazy: true
            target column: 'target_unit', lazy: true
        }
    }

    static constraints = {
        code(blank: false, size: 1..10, matches: '[a-zA-Z][a-zA-Z_0-9]*', unique: 'securityCode')
        name(blank: false, size: 1..50)
        preAddition(scale: 10)
        multiplier(scale: 10, min: 0.0000000001, max: 999999999999.9999999999)
        postAddition(scale: 10)
        target(validator: {val, obj ->
                if (val && obj.source) {
                    if (val.measure.id != obj.source.measure.id) return 'same.measure' // Must be same measure
                    if (val.scale.id == obj.source.scale.id) return 'different.scale'   // Must be different scales
                }

                return true
            })
        securityCode(validator: {val, obj ->
                obj.securityCode = obj.source.securityCode
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
