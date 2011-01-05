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

import com.whollygrails.tlc.corp.ExchangeCurrency
import doc.Line
import doc.Total
import org.apache.commons.collections.set.ListOrderedSet

class ProvisionController {

    // Injected services
    def utilService
    def bookService
    def postingService

    // Security settings
    def activities = [default: 'provntempl', template: 'provision', provide: 'provision', lines: 'provision', providing: 'provision', enquire: 'enquire']

    // List of actions with specific request types
    static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST', templateLines: 'POST', lines: 'POST', providing: 'POST']

    def index = { redirect(action: list, params: params) }

    def list = {
        def max = (params.max && params.max.toInteger() > 0) ? Math.min(params.max.toInteger(), utilService.setting('pagination.max', 50)) : utilService.setting('pagination.default', 20)
        def offset = params.offset?.toInteger() ?: 0
        def listing = TemplateDocument.findAll("from TemplateDocument as x where x.type.company = ? and x.type.type.code in ('AC', 'PR') order by x.type.code, x.description",
                [utilService.currentCompany()], [max: max, offset: offset])
        def total = TemplateDocument.executeQuery("select count(*) from TemplateDocument as x where x.type.company = ? and x.type.type.code in ('AC', 'PR')", [utilService.currentCompany()])[0]
        [templateDocumentInstanceList: listing, templateDocumentInstanceTotal: total]
    }

    def show = {
        def templateDocumentInstance = TemplateDocument.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (!templateDocumentInstance || !['AC', 'PR'].contains(templateDocumentInstance.type.type.code)) {
            flash.message = 'templateDocument.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Template not found with id ${params.id}"
            redirect(action: list)
        } else {
            return [templateDocumentInstance: templateDocumentInstance]
        }
    }

    def delete = {
        def templateDocumentInstance = TemplateDocument.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (templateDocumentInstance && ['AC', 'PR'].contains(templateDocumentInstance.type.type.code)) {
            try {
                templateDocumentInstance.delete(flush: true)
                flash.message = 'templateDocument.deleted'
                flash.args = [templateDocumentInstance.toString()]
                flash.defaultMessage = "Template ${templateDocumentInstance.toString()} deleted"
                redirect(action: list)
            } catch (Exception e) {
                flash.message = 'templateDocument.not.deleted'
                flash.args = [templateDocumentInstance.toString(), e.class.simpleName]
                flash.defaultMessage = "Template ${templateDocumentInstance.toString()} could not be deleted (${e.class.simpleName})"
                redirect(action: show, id: params.id)
            }
        } else {
            flash.message = 'templateDocument.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Template not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def edit = {
        def templateDocumentInstance = TemplateDocument.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (!templateDocumentInstance || !['AC', 'PR'].contains(templateDocumentInstance.type.type.code)) {
            flash.message = 'templateDocument.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Template not found with id ${params.id}"
            redirect(action: list)
        } else {
            for (line in templateDocumentInstance.lines) {
                if (line.account && bookService.hasAccountAccess(line.account)) {
                    line.accountCode = line.account.code
                    line.accountName = line.account.name
                }
            }

            return getTemplateModel(utilService.currentCompany(), templateDocumentInstance)
        }
    }

    def update = {
        def templateDocumentInstance = TemplateDocument.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (templateDocumentInstance && ['AC', 'PR'].contains(templateDocumentInstance.type.type.code)) {
            if (params.version) {
                def version = params.version.toLong()
                if (templateDocumentInstance.version > version) {
                    templateDocumentInstance.errors.rejectValue('version', 'templateDocument.optimistic.locking.failure', 'Another user has updated this Template while you were editing')
                    render(view: 'edit', model: getTemplateModel(utilService.currentCompany(), templateDocumentInstance))
                    return
                }
            }

            if (saveTemplate(templateDocumentInstance, params)) {
                flash.message = 'templateDocument.updated'
                flash.args = [templateDocumentInstance.toString()]
                flash.defaultMessage = "Template ${templateDocumentInstance.toString()} updated"
                redirect(action: show, id: templateDocumentInstance.id)
            } else {
                render(view: 'edit', model: getTemplateModel(utilService.currentCompany(), templateDocumentInstance))
            }
        } else {
            flash.message = 'templateDocument.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Template not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def create = {
        def templateDocumentInstance = new TemplateDocument()
        for (int i = 0; i < 10; i++) templateDocumentInstance.addToLines(new TemplateLine())
        getTemplateModel(utilService.currentCompany(), templateDocumentInstance)
    }

    def save = {
        def templateDocumentInstance = new TemplateDocument()
        if (saveTemplate(templateDocumentInstance, params)) {
            flash.message = 'templateDocument.created'
            flash.args = [templateDocumentInstance.toString()]
            flash.defaultMessage = "Template ${templateDocumentInstance.toString()} created"
            redirect(action: show, id: templateDocumentInstance.id)
        } else {
            render(view: 'create', model: getTemplateModel(utilService.currentCompany(), templateDocumentInstance))
        }
    }

    def templateLines = {
        def templateDocumentInstance
        if (params.id) {
            templateDocumentInstance = TemplateDocument.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
            if (!templateDocumentInstance || !['AC', 'PR'].contains(templateDocumentInstance.type.type.code)) {
                flash.message = 'templateDocument.not.found'
                flash.args = [params.id]
                flash.defaultMessage = "Template not found with id ${params.id}"
                redirect(action: list)
                return
            }
        } else {
            templateDocumentInstance = new TemplateDocument()
        }

        templateDocumentInstance.properties['type', 'currency', 'description', 'reference', 'sourceAdjustment'] = params

        // Need to refresh the lines due to what appears to be a bug in Grails 1.1.
        postingService.refreshLines(templateDocumentInstance, params)

        // Add the lines
        for (int i = 0; i < 10; i++) templateDocumentInstance.addToLines(new TemplateLine())

        // Grails would automatically save an existing record that was modified if we didn't discard it
        if (templateDocumentInstance.id) templateDocumentInstance.discard()

        render(view: params.view, model: getTemplateModel(utilService.currentCompany(), templateDocumentInstance))
    }

    def template = {
        def templateDocumentInstance = TemplateDocument.findByIdAndSecurityCode(params.id, utilService.currentCompany().securityCode)
        if (templateDocumentInstance && ['AC', 'PR'].contains(templateDocumentInstance.type.type.code)) {
            def documentInstance = new Document()
            documentInstance.type = templateDocumentInstance.type
            if (documentInstance.type.autoGenerate) {
                documentInstance.type.discard()
                def documentTypeInstance = DocumentType.lock(documentInstance.type.id)
                documentInstance.code = documentTypeInstance.nextSequenceNumber.toString()
                documentTypeInstance.nextSequenceNumber += 1
                documentTypeInstance.saveThis()
            }
            documentInstance.currency = templateDocumentInstance.currency
            documentInstance.reference = templateDocumentInstance.reference
            documentInstance.description = templateDocumentInstance.description
            documentInstance.sourceAdjustment = templateDocumentInstance.sourceAdjustment
            if (templateDocumentInstance.lines) {
                documentInstance.lines = new ListOrderedSet()
                def docLine
                for (line in templateDocumentInstance.lines) {
                    docLine = new Line()
                    if (line.account && bookService.hasAccountAccess(line.account)) {
                        docLine.accountCode = line.account.code
                        docLine.accountName = line.account.name
                    }

                    docLine.description = line.description
                    docLine.documentValue = line.documentValue
                    documentInstance.addToLines(docLine)
                }
            } else {

                // Add some lines
                for (int i = 0; i < 10; i++) documentInstance.addToLines(new Line())
            }

            render(view: 'provide', model: getModel(utilService.currentCompany(), documentInstance))
        } else {
            flash.message = 'templateDocument.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Template not found with id ${params.id}"
            redirect(action: provide)
        }
    }

    def provide = {
        def documentInstance = new Document()
        for (int i = 0; i < 10; i++) documentInstance.addToLines(new Line())
        getModel(utilService.currentCompany(), documentInstance)
    }

    def lines = {
        def documentInstance = new Document()
        if (!params.code && params.sourceNumber) params.code = params.sourceNumber  // A disabled field would not be in the params, so we keep a copy in a hidden field
        documentInstance.properties['type', 'period', 'currency', 'code', 'description', 'documentDate', 'reference', 'sourceAdjustment', 'sourceTotal'] = params

        // Need to refresh the lines due to what appears to be a bug in Grails 1.1.
        postingService.refreshLines(documentInstance, params)

        // Add the lines
        for (int i = 0; i < 10; i++) documentInstance.addToLines(new Line())

        render(view: 'provide', model: getModel(utilService.currentCompany(), documentInstance))
    }

    def providing = {
        def company = utilService.currentCompany()
        def companyCurrency = utilService.companyCurrency()
        def documentInstance = new Document()
        if (!params.code && params.sourceNumber) params.code = params.sourceNumber  // A disabled field would not be in the params, so we keep a copy in a hidden field
        documentInstance.properties['type', 'period', 'currency', 'code', 'description', 'documentDate', 'reference', 'sourceAdjustment', 'sourceTotal'] = params
        def documentDecs, account, temp
        def companyDecs = companyCurrency.decimals
        def now = utilService.fixDate()
        def removables = []                 // 'blank' lines that we can remove from the document just before posting
        def companyRate = 1.0    // The exchange rate we need to multiply document currency values by to get the company currency values
        def otherRates = [:]                // Other exchange rates we may use to convert from document currency values to GL account currency values
        def documentTotal = 0.0
        def companyTotal = 0.0
        def provnControl, isAccrual, reversalType, reversalPeriod

        // Process the document header, start by checking for data binding errors
        def valid = !documentInstance.hasErrors()

        // Need to refresh the lines due to what appears to be a bug in Grails 1.1. Might as well check for
        // data binding errors in the line at the same time. We do this whether the header had a fault or not
        def num = postingService.refreshLines(documentInstance, params)
        if (num) {
            documentInstance.errorMessage(code: 'document.line.data', args: [num], default: "Line ${num} has a 'data type' error")
            valid = false
        }

        // Now get on to standard validation, starting with the header: Make sure references are to the correct company objects
        if (valid) {
            utilService.verify(documentInstance, ['type', 'period', 'currency'])
            if (documentInstance.type == null || !['AC', 'PR'].contains(documentInstance.type.type.code)) {
                documentInstance.errorMessage(field: 'type', code: 'document.bad.type', default: 'Invalid document type')
                valid = false
            }

            if (documentInstance.period == null || !['open', 'adjust'].contains(documentInstance.period.status)) {
                documentInstance.errorMessage(field: 'period', code: 'document.bad.period', default: 'Invalid document period')
                valid = false
            } else if (documentInstance.period.status == 'adjust' && !documentInstance.sourceAdjustment) {
                documentInstance.errorMessage(field: 'sourceAdjustment', code: 'document.adjust.period', args: [documentInstance.period.code], default: "Period ${documentInstance.period.code} is an adjustment period and you may only post this document to it if you set the Adjustment flag")
                valid = false
            }

            if (documentInstance.currency == null) {
                documentInstance.errorMessage(field: 'currency', code: 'document.bad.currency', default: 'Invalid document currency')
                valid = false
            }
        }

        // Make sure we have the provision control account
        if (valid) {
            isAccrual = (documentInstance.type.type.code == 'AC')
            temp = isAccrual ? 'accrue' : 'prepay'
            provnControl = bookService.getControlAccount(company, temp)
            if (!provnControl) {
                documentInstance.errorMessage(code: 'document.no.control', args: [temp], default: "Could not find the ${temp} control account in the General Ledger")
                valid = false
            }
        }

        // Ensure we can find a valid reversal document type
        if (valid) {
            temp = documentInstance.type.code + 'R'
            reversalType = DocumentType.findByCompanyAndCode(company, temp)
            if (reversalType?.type?.code != (isAccrual ? 'ACR' : 'PRR')) {
                documentInstance.errorMessage(field: 'type', code: 'document.no.reverse', args: [temp], default: "No valid ${temp} reversal document type found")
                valid = false
            }
        }

        // Ensure we can find a valid following period to post the reversal to
        if (valid) {
            temp = bookService.getActivePeriods(company)
            for (int i = 0; i < temp.size(); i++) {
                if (temp[i].id == documentInstance.period.id) {
                    if (i == temp.size() - 1) {
                        documentInstance.errorMessage(field: 'period', code: 'document.next.period', args: [documentInstance.period.code], default: "No active period found after ${documentInstance.period.code} to which the reversal could be posted")
                        valid = false
                    } else {
                        reversalPeriod = temp[i + 1]
                    }

                    break
                }
            }
        }

        // Check out the dates
        if (valid) {
            if (documentInstance.documentDate < now - 365 || documentInstance.documentDate > now + 365 || documentInstance.documentDate != utilService.fixDate(documentInstance.documentDate)) {
                documentInstance.errorMessage(field: 'documentDate', code: 'document.documentDate.invalid', default: 'Document date is invalid')
                valid = false
            }
        }

        // Get any document to company exchange rates we may need
        if (valid) {
            documentDecs = documentInstance.currency.decimals
            if (companyCurrency.code != documentInstance.currency.code) {
                companyRate = utilService.getExchangeRate(documentInstance.currency, companyCurrency, now)
                if (!companyRate) {
                    documentInstance.errorMessage(code: 'document.bad.exchangeRate', args: [documentInstance.currency.code, companyCurrency.code],
                            default: "No exchange rate available from ${documentInstance.currency.code} to ${companyCurrency.code}")
                    valid = false
                }
            }
        }

        // Step through each line checking it in detail
        if (valid) {
            num = 0
            for (line in documentInstance.lines) {
                num++

                // If this is intended to be an active line
                if (line.accountCode) {

                    // Make sure the GL account code is expanded for mnemonics and case is correct
                    temp = bookService.expandAccountCode(utilService.currentUser(), line.accountCode)
                    if (!temp) {
                        temp = message(code: 'account.not.exists', default: 'Invalid GL account')
                        documentInstance.errorMessage(code: 'document.line.message', args: [num, temp], default: "Line ${num}: ${temp}")
                        line.errors.rejectValue('accountCode', null)
                        valid = false
                        break
                    }

                    line.accountCode = temp

                    // See if the GL account actually exists
                    account = bookService.getAccount(company, line.accountCode)
                    if (account instanceof String) {
                        documentInstance.errorMessage(code: 'document.line.message', args: [num, account], default: "Line ${num}: ${account}")
                        line.errors.rejectValue('accountCode', null)
                        valid = false
                        break
                    }

                    // Make sure the GL account is active and that the user is allowed to access this account and
                    // that the account is not restricted as to what sort of documents can be posted to it
                    if (account?.active && bookService.hasAccountAccess(account)) {
                        valid = postingService.canPostDocumentToAccount(documentInstance, line, num, account)
                        if (!valid) break
                    } else {
                        temp = message(code: 'account.not.exists', default: 'Invalid GL account')
                        documentInstance.errorMessage(code: 'document.line.message', args: [num, temp], default: "Line ${num}: ${temp}")
                        line.errors.rejectValue('accountCode', null)
                        valid = false
                        break
                    }

                    // Check they have entered a value for the line
                    if (!line.documentValue) {
                        temp = message(code: 'document.zero.entry', default: 'The line value cannot be zero')
                        documentInstance.errorMessage(code: 'document.line.message', args: [num, temp], default: "Line ${num}: ${temp}")
                        line.errors.rejectValue('documentValue', null)
                        valid = false
                        break
                    }

                    // Round the values and do the totalling
                    line.documentValue = utilService.round(line.documentValue, documentDecs)
                    documentTotal += line.documentValue

                    // Set up the line ready for posting. Note that we store the account as a transient in the line
                    // record. This allows us to avoid loading the balance record to which the line will actually
                    // belong but still allows the posting routine fast access to the balance record (via account
                    // and period). We don't want to load the balance record because this can lead to attempts to
                    // update a stale balance since we don't yet have the company locked for posting.
                    line.account = account
                    line.companyValue = utilService.round(line.documentValue * companyRate, companyDecs)
                    companyTotal += line.companyValue
                    line.adjustment = documentInstance.sourceAdjustment
                    if (account.currency.code == companyCurrency.code) {
                        line.generalValue = line.companyValue
                    } else if (account.currency.code == documentInstance.currency.code) {
                        line.generalValue = line.documentValue
                    } else {
                        temp = otherRates.get(account.currency.code)
                        if (!temp) {
                            temp = utilService.getExchangeRate(documentInstance.currency, account.currency, now)
                            if (!temp) {
                                documentInstance.errorMessage(code: 'document.bad.exchangeRate', args: [documentInstance.currency.code, account.currency.code],
                                        default: "No exchange rate available from ${documentInstance.currency.code} to ${account.currency.code}")
                                valid = false
                                break
                            }

                            otherRates.put(account.currency.code, temp)
                        }

                        line.generalValue = utilService.round(line.documentValue * temp, account.currency.decimals)
                    }
                } else {

                    // These are non-active lines that, if the document passes all our checks here (other than final
                    // validation on call to the save method), we will remove before acutally saving the document.
                    removables << line
                }
            }
        }

        // Cross check the lines to the header total
        if (valid) {
            if (documentInstance.sourceTotal != null) documentInstance.sourceTotal = utilService.round(documentInstance.sourceTotal, documentDecs)
            if (documentTotal != documentInstance.sourceTotal) {
                documentInstance.errorMessage(field: 'sourceTotal', code: 'document.total.mismatch', default: 'The document total value does not agree to the sum of the total values of the lines')
                valid = false
            }
        }

        // Create total line, remove any 'blank' lines, create the reversal and then and then post the two documents
        if (valid) {
            documentInstance.addToTotal(new Total(account: provnControl, description: documentInstance.description, documentValue: documentTotal,
                    generalValue: companyTotal, companyValue: companyTotal, adjustment: documentInstance.sourceAdjustment))

            for (line in removables) {
                documentInstance.removeFromLines(line)
            }

            def reversalInstance = new Document(currency: documentInstance.currency, type: reversalType, period: reversalPeriod, code: documentInstance.code,
                    description: documentInstance.description, documentDate: documentInstance.documentDate, reference: documentInstance.reference)
            reversalInstance.lines = new ListOrderedSet()

            for (line in documentInstance.lines) {
                reversalInstance.addToLines(new Line(account: line.account, description: line.description, documentValue: line.documentValue,
                        generalValue: line.generalValue, companyValue: line.companyValue, adjustment: line.adjustment))
            }

            reversalInstance.addToTotal(new Total(account: provnControl, description: documentInstance.description, documentValue: documentTotal,
                    generalValue: companyTotal, companyValue: companyTotal, adjustment: documentInstance.sourceAdjustment))

            // This outer lock (the posting service will inner-lock for each document) is needed so that
            // we can guarantee a stable situation for BOTH documents to be posted since, without this,
            // it is possible that a database deadlock could occur
            def lock = bookService.getCompanyLock(company)
            lock.lock()
            try {
                Document.withTransaction {status ->
                    valid = postingService.post(documentInstance, status)
                    if (valid) {
                        valid = postingService.post(reversalInstance, status)
                        if (!valid) {
                            documentInstance.errorMessage(code: 'document.reversal.error', default: 'Error posting the reversal')
                            status.setRollbackOnly()
                        }
                    } else {
                        status.setRollbackOnly()
                    }
                }
            } finally {
                lock.unlock()
            }
        }

        if (valid) {           // Document comes back from posting in its debit/credit form
            flash.message = 'document.created'
            flash.args = [documentInstance.type.code, documentInstance.code]
            flash.defaultMessage = "Document ${documentInstance.type.code}${documentInstance.code} created"
            redirect(action: 'provide')
        } else {               // Document comes back in its data entry form if posting failed
            render(view: 'provide', model: getModel(company, documentInstance))
        }
    }

    def enquire = {
        def model = bookService.loadDocumentModel(params, ['AC', 'PR', 'ACR', 'PRR'])
        def documentInstance = model.documentInstance
        if (documentInstance.id) {
            model.totalInstance = bookService.getTotalLine(documentInstance)
            for (line in documentInstance.lines) {
                documentInstance.sourceAdjustment = line.adjustment
                break
            }
        }

        model
    }

// --------------------------------------------- Support Methods ---------------------------------------------

    private getModel(company, documentInstance) {
        def documentTypeList = DocumentType.findAll("from DocumentType as dt where dt.company = ? and dt.type.code in ('AC', 'PR')", [company])
        def periodList = bookService.getActivePeriods(company)
        def currencyList = ExchangeCurrency.findAllByCompany(company, [cache: true])
        if (!documentInstance.documentDate) {
            documentInstance.documentDate = utilService.fixDate()
            documentInstance.period = bookService.selectPeriod(periodList, documentInstance.documentDate)
        }

        if (!documentInstance.currency) documentInstance.currency = utilService.companyCurrency()
        periodList = periodList.reverse()
        def settings = [:]
        settings.codeGenerate = documentInstance.type?.autoGenerate
        settings.codeEdit = documentInstance.type?.allowEdit
        settings.decimals = documentInstance.currency.decimals
        return [documentInstance: documentInstance, documentTypeList: documentTypeList, periodList: periodList, currencyList: currencyList, settings: settings]
    }

    private getTemplateModel(company, templateDocumentInstance) {
        def documentTypeList = DocumentType.findAll("from DocumentType as dt where dt.company = ? and dt.type.code in ('AC', 'PR')", [company])
        def currencyList = ExchangeCurrency.findAllByCompany(company, [cache: true])
        if (!templateDocumentInstance.currency) templateDocumentInstance.currency = utilService.companyCurrency()
        def settings = [:]
        settings.decimals = templateDocumentInstance.currency.decimals
        return [templateDocumentInstance: templateDocumentInstance, documentTypeList: documentTypeList, currencyList: currencyList, settings: settings]
    }

    private saveTemplate(templateDocumentInstance, params) {
        templateDocumentInstance.properties['type', 'currency', 'description', 'reference', 'sourceAdjustment'] = params
        utilService.verify(templateDocumentInstance, ['type', 'currency'])             // Ensure correct references
        def valid = !templateDocumentInstance.hasErrors()
        def removables = []
        def documentDecs = templateDocumentInstance.currency?.decimals
        def account, temp

        // Need to refresh the lines due to what appears to be a bug in Grails 1.1. Might as well check for
        // data binding errors in the line at the same time. We do this whether the header had a fault or not
        def num = postingService.refreshLines(templateDocumentInstance, params)
        if (num) {
            templateDocumentInstance.errorMessage(code: 'document.line.data', args: [num], default: "Line ${num} has a 'data type' error")
            valid = false
        } else {
            num = 0
            for (line in templateDocumentInstance.lines) {
                num++
                if (line.accountCode) {

                    // Make sure the GL account code is expanded for mnemonics and case is correct
                    temp = bookService.expandAccountCode(utilService.currentUser(), line.accountCode)
                    if (!temp) {
                        temp = message(code: 'account.not.exists', default: 'Invalid GL account')
                        templateDocumentInstance.errorMessage(code: 'document.line.message', args: [num, temp], default: "Line ${num}: ${temp}")
                        line.errors.rejectValue('accountCode', null)
                        valid = false
                        break
                    } else {
                        line.accountCode = temp
                    }

                    // See if the GL account actually exists
                    account = bookService.getAccount(utilService.currentCompany(), line.accountCode)
                    if (account instanceof String) {
                        templateDocumentInstance.errorMessage(code: 'document.line.message', args: [num, account], default: "Line ${num}: ${account}")
                        line.errors.rejectValue('accountCode', null)
                        valid = false
                        break
                    } else {

                        // Make sure the GL account is active and that the user is allowed to access this account and
                        // that the account is not restricted as to what sort of documents can be posted to it
                        if (account?.active && bookService.hasAccountAccess(account)) {
                            valid = postingService.canPostDocumentToAccount(templateDocumentInstance, line, num, account)
                            if (!valid) break
                        } else {
                            temp = message(code: 'account.not.exists', default: 'Invalid GL account')
                            templateDocumentInstance.errorMessage(code: 'document.line.message', args: [num, temp], default: "Line ${num}: ${temp}")
                            line.errors.rejectValue('accountCode', null)
                            valid = false
                            break
                        }

                        // Check they haven't entered BOTH a debit AND a credit value for the line
                        if (line.documentDebit != null && line.documentCredit != null) {
                            temp = message(code: 'document.dup.entry', default: 'You may not enter both a Debit and Credit value')
                            templateDocumentInstance.errorMessage(code: 'document.line.message', args: [num, temp], default: "Line ${num}: ${temp}")
                            line.errors.rejectValue('documentDebit', null)
                            valid = false
                            break
                        }

                        // Round any entered values
                        if (line.documentDebit != null) {
                            line.documentDebit = utilService.round(line.documentDebit, documentDecs)
                            line.documentValue = line.documentDebit
                        } else if (line.documentCredit != null) {
                            line.documentCredit = utilService.round(line.documentCredit, documentDecs)
                            line.documentValue = -line.documentCredit
                        }

                        line.account = account
                    }
                } else {
                    removables << line
                }
            }
        }

        // Remove any 'blank' lines and then save the document
        if (valid) {
            for (line in removables) {
                templateDocumentInstance.removeFromLines(line)

                // Need to delete the items as removing them from the association dosn't do it
                if (line.id) {
                    line.delete(flush: true)
                    line.discard()
                }
            }

            valid = templateDocumentInstance.save(flush: true)  // With deep validation
        }

        return valid
    }
}
