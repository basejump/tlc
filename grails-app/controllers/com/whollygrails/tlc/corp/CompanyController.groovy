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

import com.whollygrails.tlc.books.GeneralBalance
import com.whollygrails.tlc.books.GeneralTransaction
import com.whollygrails.tlc.books.Period
import com.whollygrails.tlc.books.Year
import com.whollygrails.tlc.sys.SystemCountry
import com.whollygrails.tlc.sys.SystemCurrency
import com.whollygrails.tlc.sys.SystemLanguage
import com.whollygrails.tlc.sys.SystemRole
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class CompanyController {

    // Injected services
    def utilService
    def bookService
    def sessionFactory

    // Security settings
    def activities = [default: 'sysadmin', details: 'coadmin', modify: 'coadmin', logo: 'coadmin', upload: 'coadmin']

    // List of actions with specific request types
    static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST', modify: 'POST', upload: 'POST', process: 'POST']

    def index = { redirect(action: list, params: params) }

    def list = {
        params.max = (params.max && params.max.toInteger() > 0) ? Math.min(params.max.toInteger(), utilService.setting('pagination.max', 50)) : utilService.setting('pagination.default', 20)
        params.sort = ['name', 'systemOnly'].contains(params.sort) ? params.sort : 'name'
        def lst = Company.selectList()
        lst.each {
            it.displayCurrency = ExchangeCurrency.findByCompanyAndCompanyCurrency(it, true, [cache: true])
            it.displayTaxCode = TaxCode.findByCompanyAndCompanyTaxCode(it, true, [cache: true])
        }
        [companyInstanceList: lst, companyInstanceTotal: Company.selectCount()]
    }

    def show = {
        def companyInstance = Company.get(params.id)
        if (!companyInstance) {
            flash.message = 'company.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Company not found with id ${params.id}"
            redirect(action: list)
        } else {
            companyInstance.displayCurrency = ExchangeCurrency.findByCompanyAndCompanyCurrency(companyInstance, true, [cache: true])
            companyInstance.displayTaxCode = TaxCode.findByCompanyAndCompanyTaxCode(companyInstance, true, [cache: true])
            return [companyInstance: companyInstance]
        }
    }

    def delete = {
        def companyInstance = Company.get(params.id)
        if (companyInstance) {
            if (companyInstance.systemOnly) {
                flash.message = 'company.system.delete'
                flash.defaultMessage = 'You may not delete the system company'
                redirect(action: show, id: params.id)
            } else if (companyInstance.id == utilService.currentCompany()?.id) {
                flash.message = 'company.system.self'
                flash.defaultMessage = 'You may not delete the company you are currently logged in to'
                redirect(action: show, id: params.id)
            } else {

                // If there are any transaction, do it as a background task after getting them to confirm it
                def yrs = Year.findAllByCompany(companyInstance)
                for (yr in yrs) {
                    def pds = Period.findAllByYear(yr)
                    for (pd in pds) {
                        def bals = GeneralBalance.findAllByPeriod(pd)
                        for (bal in bals) {
                            if (GeneralTransaction.countByBalance(bal)) {
                                redirect(action: confirm, id: params.id)
                                return
                            }
                        }
                    }
                }

                companyInstance.prepareForDeletion(sessionFactory.currentSession)    // Notice not in a transaction
                def deleted = false
                def caughtException
                Company.withTransaction {status ->
                    try {
                        bookService.deleteCompany(companyInstance)
                        utilService.cacheService.clearAll(companyInstance.securityCode)
                        def logo = utilService.realFile("/images/logos/L${companyInstance.securityCode}.png")
                        if (logo.exists()) logo.delete()
                        deleted = true
                    } catch (Exception e) {
                        log.error(e)
                        caughtException = e
                        status.setRollbackOnly()
                    }
                }

                if (deleted) {
                    flash.message = 'company.deleted'
                    flash.args = [companyInstance.toString()]
                    flash.defaultMessage = "Company ${companyInstance.toString()} deleted"
                    redirect(action: list)
                } else {
                    flash.message = 'company.not.deleted'
                    flash.args = [companyInstance.toString(), caughtException.class.simpleName]
                    flash.defaultMessage = "Company ${companyInstance.toString()} could not be deleted (${caughtException.class.simpleName})"
                    redirect(action: show, id: params.id)
                }
            }
        } else {
            flash.message = 'company.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Company not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def edit = {
        def companyInstance = Company.get(params.id)
        if (!companyInstance) {
            flash.message = 'company.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Company not found with id ${params.id}"
            redirect(action: list)
        } else {
            companyInstance.displayCurrency = ExchangeCurrency.findByCompanyAndCompanyCurrency(companyInstance, true, [cache: true])
            companyInstance.displayTaxCode = TaxCode.findByCompanyAndCompanyTaxCode(companyInstance, true, [cache: true])
            return [companyInstance: companyInstance]
        }
    }

    def update = {
        def companyInstance = Company.get(params.id)
        if (companyInstance) {
            companyInstance.displayCurrency = ExchangeCurrency.findByCompanyAndCompanyCurrency(companyInstance, true, [cache: true])
            companyInstance.displayTaxCode = TaxCode.findByCompanyAndCompanyTaxCode(companyInstance, true, [cache: true])
            if (params.version) {
                def version = params.version.toLong()
                if (companyInstance.version > version) {
                    companyInstance.errors.rejectValue('version', 'company.optimistic.locking.failure', 'Another user has updated this Company while you were editing')
                    render(view: 'edit', model: [companyInstance: companyInstance])
                    return
                }
            }

            def oldTaxCode = companyInstance.displayTaxCode
            companyInstance.displayTaxCode = (oldTaxCode.id == params.displayTaxCode.id?.toInteger()) ? oldTaxCode : TaxCode.findByIdAndCompany(params.displayTaxCode.id, companyInstance)
            companyInstance.properties['name', 'country', 'language'] = params
            def valid = !companyInstance.hasErrors()
            if (valid) {
                def newTaxCode = companyInstance.displayTaxCode
                Company.withTransaction {status ->
                    if (companyInstance.saveThis()) {
                        if (oldTaxCode.id != newTaxCode.id) {
                            if (oldTaxCode) {
                                oldTaxCode.companyTaxCode = false
                                if (!oldTaxCode.saveThis()) {
                                    companyInstance.errorMessage(code: 'company.taxcode.update.error', default: 'Unable to update the company tax code')
                                    valid = false
                                    status.setRollbackOnly()
                                }
                            }

                            if (valid) {
                                newTaxCode.companyTaxCode = true
                                if (!newTaxCode.saveThis()) {
                                    companyInstance.errorMessage(code: 'company.taxcode.update.error', default: 'Unable to update the company tax code')
                                    valid = false
                                    status.setRollbackOnly()
                                }
                            }
                        }
                    } else {
                        valid = false
                        status.setRollbackOnly()
                    }
                }
            }

            if (valid) {
                flash.message = 'company.updated'
                flash.args = [companyInstance.toString()]
                flash.defaultMessage = "Company ${companyInstance.toString()} updated"
                redirect(action: show, id: companyInstance.id)
            } else {
                render(view: 'edit', model: [companyInstance: companyInstance])
            }
        } else {
            flash.message = 'company.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Company not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def create = {
        def companyInstance = new Company()
        def locale = utilService.currentLocale()
        def language = SystemLanguage.findByCode(locale.getLanguage() ?: 'en') ?: SystemLanguage.findByCode('en')
        def country = SystemCountry.findByCode(locale.getCountry() ?: ((language.code == 'en') ? 'US' : language.code.toUpperCase())) ?: SystemCountry.findByCode('US')
        companyInstance.language = language
        companyInstance.country = country
        companyInstance.currency = country.currency
        companyInstance.loadDemo = utilService.systemSetting('isDemoSystem')

        // The US, Canada, Mexico, Columbia, Venzuela, Chile and the Phillippines use US Letter stationery
        // but we let the PDF system handle the resizing and so the stationery property has been removed
        //if (['US', 'CA', 'MX', 'CO', 'VE', 'CL', 'PH'].contains(country.code)) companyInstance.stationery = 'letter'

        return [companyInstance: companyInstance]
    }

    def save = {
        def companyInstance = new Company()
        companyInstance.properties['name', 'country', 'language', 'loadDemo'] = params
        def currency = SystemCurrency.get(params.currency.id)
        companyInstance.currency = currency
        def rateValid = true
        def valid = !companyInstance.hasErrors()
        if (valid) {
            def rate = 1.0
            if (currency.code != utilService.BASE_CURRENCY_CODE) {
                if (currency.autoUpdate) {
                    rate = utilService.readURL("http://finance.yahoo.com/d/quotes.csv?s=${utilService.BASE_CURRENCY_CODE}${currency.code}=X&f=b")

                    try {
                        rate = utilService.round(new BigDecimal(rate), 6)
                    } catch (NumberFormatException nfe) {
                        rate = 1.0
                        ratevalid = false
                    }
                } else {
                    ratevalid = false
                }
            }

            // Now create the record and its initial data
            Company.withTransaction {status ->
                if (companyInstance.saveThis()) {
                    def user = utilService.currentUser()
                    def companyUser = new CompanyUser(company: companyInstance, user: user)
                    if (companyUser.saveThis()) {
                        companyUser.addToRoles(SystemRole.findByCode('companyAdmin'))
                        if (companyUser.save()) {       // With deep validation
                            def result = companyInstance.initializeData(user, currency, rate, companyInstance.loadDemo)
                            if (result != null) {
                                companyInstance.errorMessage(code: 'company.initialization.error', args: [result], default: "Unable to load the initial company data for ${result}")
                                status.setRollbackOnly()
                                valid = false
                            }
                        } else {
                            companyInstance.errorMessage(code: 'company.role.error', default: 'Unable to set the company administration role for the initial user')
                            status.setRollbackOnly()
                            valid = false
                        }
                    } else {
                        companyInstance.errorMessage(code: 'company.user.error', default: 'Unable to create the initial company user')
                        status.setRollbackOnly()
                        valid = false
                    }
                } else {
                    status.setRollbackOnly()
                    valid = false
                }
            }
        }

        if (valid) {
            if (rateValid) {
                flash.message = 'company.created'
                flash.args = [companyInstance.toString()]
                flash.defaultMessage = "Company ${companyInstance.toString()} created"
            } else {
                flash.message = 'company.created.warning'
                flash.args = ["${companyInstance.id}", currency.name]
                flash.defaultMessage = "Company ${companyInstance.toString()} created. NOTE that if you intend to use foreign currencies, you should now set the exchange rate for ${currency.name}."
            }
            redirect(action: show, id: companyInstance.id)
        } else {
            render(view: 'create', model: [companyInstance: companyInstance])
        }
    }

    def details = {
        def companyInstance = utilService.currentCompany()
        if (!companyInstance) {
            flash.message = 'company.not.found'
            flash.args = [utilService.currentCompany()?.id]
            flash.defaultMessage = "Company not found with id ${utilService.currentCompany()?.id}}"
            redirect(controller: 'systemMenu', action: 'display')
        } else {
            companyInstance.displayCurrency = ExchangeCurrency.findByCompanyAndCompanyCurrency(companyInstance, true, [cache: true])
            companyInstance.displayTaxCode = TaxCode.findByCompanyAndCompanyTaxCode(companyInstance, true, [cache: true])
            return [companyInstance: companyInstance]
        }
    }

    def modify = {
        def companyInstance = utilService.currentCompany()
        if (companyInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (companyInstance.version > version) {
                    companyInstance.errors.rejectValue('version', 'company.optimistic.locking.failure', 'Another user has updated this Company while you were editing')
                    redirect(controller: 'systemMenu', action: 'display')
                    return
                }
            }

            def oldTaxCode = TaxCode.findByCompanyAndCompanyTaxCode(companyInstance, true, [cache: true])
            companyInstance.displayTaxCode = (oldTaxCode.id == params.displayTaxCode.id?.toInteger()) ? oldTaxCode : TaxCode.findByIdAndCompany(params.displayTaxCode.id, companyInstance)
            companyInstance.displayCurrency = ExchangeCurrency.findByCompanyAndCompanyCurrency(companyInstance, true, [cache: true])
            companyInstance.properties['name', 'country', 'language'] = params
            def valid = !companyInstance.hasErrors()
            if (valid) {
                def newTaxCode = companyInstance.displayTaxCode
                Company.withTransaction {status ->
                    if (companyInstance.saveThis()) {
                        if (oldTaxCode.id != newTaxCode.id) {
                            if (oldTaxCode) {
                                oldTaxCode.companyTaxCode = false
                                if (!oldTaxCode.saveThis()) {
                                    companyInstance.errorMessage(code: 'company.taxcode.update.error', default: 'Unable to update the company tax code')
                                    valid = false
                                    status.setRollbackOnly()
                                }
                            }

                            if (valid) {
                                newTaxCode.companyTaxCode = true
                                if (!newTaxCode.saveThis()) {
                                    companyInstance.errorMessage(code: 'company.taxcode.update.error', default: 'Unable to update the company tax code')
                                    valid = false
                                    status.setRollbackOnly()
                                }
                            }
                        }
                    } else {
                        valid = false
                        status.setRollbackOnly()
                    }
                }
            }

            if (valid) {
                flash.message = 'company.updated'
                flash.args = [companyInstance.toString()]
                flash.defaultMessage = "Company ${companyInstance.toString()} updated"
            }

            render(view: 'details', model: [companyInstance: companyInstance])
        } else {
            flash.message = 'company.not.found'
            flash.args = [utilService.currentCompany()?.id]
            flash.defaultMessage = "Company not found with id ${utilService.currentCompany()?.id}"
            redirect(controller: 'systemMenu', action: 'display')
        }
    }

    def logo = {
        [companyInstance: utilService.currentCompany()]
    }

    def upload = {
        def companyInstance = utilService.currentCompany()
        def valid = true
        def uploadFile = request.getFile('file')
        if (uploadFile.isEmpty()) {
            companyInstance.errorMessage(code: 'company.logo.empty', default: 'File is empty')
            valid = false
        } else {
            if (uploadFile.getSize() > 1024 * 1024) {
                companyInstance.errorMessage(code: 'company.logo.size', default: 'File exceeds the 1 MB limit')
                valid = false
            } else {
                def name = uploadFile.getOriginalFilename()
                def pos = name.lastIndexOf('.')
                if (pos <= 0 && pos > name.length() - 4) {
                    companyInstance.errorMessage(code: 'company.logo.suffix', default: 'File does not have a recognized suffix (.png etc)')
                    valid = false
                } else {
                    name = name.substring(pos + 1).toLowerCase()
                    if (name == 'bmp' || name == 'gif' || name == 'jpg' || name == 'jpeg' || name == 'png') {
                        name = "/temp/L${companyInstance.securityCode}.${name}"
                        def sourceFile = utilService.realFile(name)
                        try {
                            uploadFile.transferTo(sourceFile)
                            def targetFile = utilService.realFile("/images/logos/L${companyInstance.securityCode}.png")
                            if (!createLogo(sourceFile, targetFile)) {
                                companyInstance.errorMessage(code: 'company.logo.bad.create', default: 'Unable to create the logo from the selected image')
                                valid = false
                            }

                            try {
                                sourceFile.delete()
                            } catch (Exception e1) {}

                            if (!valid) {
                                try {
                                    targetFile.delete()
                                } catch (Exception e2) {}
                            }
                        } catch (Exception ex) {
                            log.error(ex)
                            companyInstance.errorMessage(code: 'company.logo.bad.upload', default: 'Unable to upload the file')
                            valid = false
                        }
                    } else {
                        companyInstance.errorMessage(code: 'company.logo.suffix', default: 'File does not have a recognized suffix (.png etc)')
                        valid = false
                    }
                }
            }
        }

        if (valid) {
            utilService.clearCurrentLogo()
            flash.message = 'company.logo.updated'
            flash.defaultMessage = 'Company logo updated'
            redirect(controller: 'systemMenu', action: 'display')
        } else {
            render(view: 'logo', model: [companyInstance: companyInstance])
        }
    }

    def confirm = {
        [companyInstance: Company.get(params.id)]
    }

    def process = {
        def companyInstance = Company.get(params.id)
        if (companyInstance) {
            if (companyInstance.systemOnly) {
                flash.message = 'company.system.delete'
                flash.defaultMessage = 'You may not delete the system company'
            } else {
                def result = utilService.demandRunFromParams('delCompany', [p_stringId: companyInstance.id.toString(), preferredStart: params.preferredStart])
                if (result instanceof String) {
                    flash.message = result
                    redirect(action: confirm, id: companyInstance.id)
                    return
                }

                flash.message = 'queuedTask.demand.good'
                flash.args = [result]
                flash.defaultMessage = "The task has been placed in the queue for execution as task number ${result}"
            }
        } else {
            flash.message = 'company.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Company not found with id ${params.id}"
        }

        redirect(action: list)
    }

    // --------------------------------------------- Support Methods ---------------------------------------------

    private createLogo(sourceFile, targetFile) {
        BufferedImage source
        try {
            source = ImageIO.read(sourceFile)
        } catch (Exception e1) {
            log.error(e1)
            return false
        }

        if (source.getWidth() != 48 || source.getHeight() != 48) {
            BufferedImage target = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
            Graphics2D g = (Graphics2D) target.createGraphics()
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g.drawImage(source, 0, 0, 48, 48, null)
            } finally {
                g.dispose()
            }

            source = target
        }

        try {
            ImageIO.write(source, 'png', targetFile)
        } catch (Exception e2) {
            log.error(e2)
            return false
        }

        return true
    }
}