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
package com.whollygrails.tlc.tasks

import com.whollygrails.tlc.sys.TaskExecutable
import grails.util.Environment

public class UnauthorizedAdvicesReport extends TaskExecutable {

    def execute() {
        def reportParams = [:]
        def accessCodeList = bookService.supplierAccessCodes(company, user)
        if (accessCodeList) {
            def codeList = ''
            for (int i = 0; i < accessCodeList.size(); i++) {
                if (i) codeList += ','
                codeList += accessCodeList[i].id.toString()
            }

            reportParams.codeList = codeList
        } else {
            completionMessage = message(code: 'report.no.access', default: 'You do not have permission to access any accounts and therefore cannot run this report.')
            return false
        }

        def title = message(code: 'remittance.unauthorized.title', default: 'Unauthorized Remittance Advices')
        reportParams.reportTitle = title
        reportParams.txtBank = message(code: 'remittance.unauthorized.bank', default: 'Bank')
        reportParams.txtCurrency = message(code: 'remittance.currency', default: 'Currency')
        reportParams.txtSupplier = message(code: 'remittance.code', default: 'Supplier Code')
        reportParams.txtName = message(code: 'supplier.name', default: 'Name')
        reportParams.txtDate = message(code: 'remittance.adviceDate', default: 'Advice Date')
        reportParams.txtAmount = message(code: 'remittance.total', default: 'Payment')
        yield()
        def pdfFile = createReportPDF('UnauthorizedAdvices', reportParams)
        yield()
        utilService.sendMail {
            to user.email
            subject title
            body(view: '/emails/genericReport', model: [companyInstance: company, systemUserInstance: user, title: title])
            attach pdfFile
        }

        if (Environment.current != Environment.DEVELOPMENT) pdfFile.delete()

        return true
    }
}