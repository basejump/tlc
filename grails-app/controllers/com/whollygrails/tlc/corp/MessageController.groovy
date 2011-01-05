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

import com.whollygrails.tlc.sys.*

class MessageController {

    // Injected services
    def utilService

    // Security settings
    def activities = [default: 'coadmin']

    // List of actions with specific request types
    static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST', output: 'POST', load: 'POST']

    def index = { redirect(action: list, params: params) }

    def list = {
        params.max = (params.max && params.max.toInteger() > 0) ? Math.min(params.max.toInteger(), utilService.setting('pagination.max', 50)) : utilService.setting('pagination.default', 20)
        params.sort = ['code', 'locale', 'text'].contains(params.sort) ? params.sort : 'code'
        [messageInstanceList: Message.selectList(company: utilService.currentCompany()), messageInstanceTotal: Message.selectCount()]
    }

    def show = {
        def messageInstance = Message.findByIdAndCompany(params.id, utilService.currentCompany())
        if (!messageInstance) {
            flash.message = 'message.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Message not found with id ${params.id}"
            redirect(action: list)
        } else {
            return [messageInstance: messageInstance]
        }
    }

    def delete = {
        def messageInstance = Message.findByIdAndCompany(params.id, utilService.currentCompany())
        if (messageInstance) {
            try {
                messageInstance.delete(flush: true)
                utilService.cacheService.resetThis('message', utilService.currentCompany().securityCode, messageInstance.code)
                flash.message = 'message.deleted'
                flash.args = [messageInstance.toString()]
                flash.defaultMessage = "Message ${messageInstance.toString()} deleted"
                redirect(action: list)
            } catch (Exception e) {
                flash.message = 'message.not.deleted'
                flash.args = [messageInstance.toString(), e.class.simpleName]
                flash.defaultMessage = "Message ${messageInstance.toString()} could not be deleted (${e.class.simpleName})"
                redirect(action: show, id: params.id)
            }
        } else {
            flash.message = 'message.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Message not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def edit = {
        def messageInstance = Message.findByIdAndCompany(params.id, utilService.currentCompany())
        if (!messageInstance) {
            flash.message = 'message.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Message not found with id ${params.id}"
            redirect(action: list)
        } else {
            return [messageInstance: messageInstance]
        }
    }

    def update = {
        def messageInstance = Message.findByIdAndCompany(params.id, utilService.currentCompany())
        if (messageInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (messageInstance.version > version) {
                    messageInstance.errors.rejectValue('version', 'message.optimistic.locking.failure', 'Another user has updated this Message while you were editing')
                    render(view: 'edit', model: [messageInstance: messageInstance])
                    return
                }
            }

            def oldCode = messageInstance.code
            def oldLocale = messageInstance.locale
            messageInstance.properties['code', 'locale', 'text'] = params
            def valid = !messageInstance.hasErrors()
            if (valid && messageInstance.locale != oldLocale && !localeIsValid(messageInstance.locale)) {
                messageInstance.errorMessage(field: 'locale', code: 'message.bad.locale', default: 'Invalid locale')
                valid = false
            }

            if (valid) valid = messageInstance.saveThis()
            if (valid) {
                if (messageInstance.code != oldCode) utilService.cacheService.resetThis('message', utilService.currentCompany().securityCode, oldCode)
                utilService.cacheService.resetThis('message', utilService.currentCompany().securityCode, messageInstance.code)
                flash.message = 'message.updated'
                flash.args = [messageInstance.toString()]
                flash.defaultMessage = "Message ${messageInstance.toString()} updated"
                redirect(action: show, id: messageInstance.id)
            } else {
                render(view: 'edit', model: [messageInstance: messageInstance])
            }
        } else {
            flash.message = 'message.not.found'
            flash.args = [params.id]
            flash.defaultMessage = "Message not found with id ${params.id}"
            redirect(action: list)
        }
    }

    def create = {
        def messageInstance = new Message()
        messageInstance.company = utilService.currentCompany()
        return [messageInstance: messageInstance]
    }

    def save = {
        def messageInstance = new Message()
        messageInstance.properties['code', 'locale', 'text'] = params
        messageInstance.company = utilService.currentCompany()
        def valid = !messageInstance.hasErrors()
        if (valid && !localeIsValid(messageInstance.locale)) {
            messageInstance.errorMessage(field: 'locale', code: 'message.bad.locale', default: 'Invalid locale')
            valid = false
        }

        if (valid) valid = messageInstance.saveThis()
        if (valid) {
            utilService.cacheService.resetThis('message', utilService.currentCompany().securityCode, messageInstance.code)
            flash.message = 'message.created'
            flash.args = [messageInstance.toString()]
            flash.defaultMessage = "Message ${messageInstance.toString()} created"
            redirect(action: show, id: messageInstance.id)
        } else {
            render(view: 'create', model: [messageInstance: messageInstance])
        }
    }

    def export = {
        [currentRecordCount: [count: Message.countByCompany(utilService.currentCompany())]]
    }

    def output = {
        def dir = new File(new File(servletContext.getRealPath('/')).getParent(), "grails-app${File.separator}i18n")
        def map = [:]
        Message.findAllByCompany(utilService.currentCompany()).each {
            def list = map.get(it.locale)
            if (list == null) {
                list = []
                map.put(it.locale, list)
            }

            def code = it.code.trim()
            def text = it.text.trim()

            list << [code: ((code.indexOf('.') == -1) ? CacheService.IMPOSSIBLE_VALUE + code : code), text: text]
        }

        def fileCount = 0
        def recordCount = 0
        map.each {entry ->
            def locale = entry.key
            def list = entry.value
            list.sort {it.code}

            if (locale == '*') {
                locale = ''
            } else if (locale.length() == 2) {
                locale = '_' + locale
            } else {
                locale = '_' + locale.substring(0, 2) + '_' + locale.substring(2)
            }

            def file = new File(dir, "company${locale}.properties")
            def out = null
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), 'UTF-8'))
            fileCount++
            def prefix = ''
            try {
                list.each {line ->
                    recordCount++
                    def code, first
                    if (line.code.startsWith(CacheService.IMPOSSIBLE_VALUE)) {
                        code = line.code.substring(CacheService.IMPOSSIBLE_VALUE.length())
                        first = ''
                    } else {
                        code = line.code
                        first = code.substring(0, code.indexOf('.'))
                    }

                    if (first != prefix) {
                        out.newLine()
                        prefix = first
                    }

                    first = true
                    SystemMessage.breakLine(code, line.text).each {text ->
                        if (first) {
                            out.write("${code}=${text}")
                            first = false
                        } else {
                            out.write(text)
                        }

                        out.newLine()
                    }
                }
            } finally {
                if (out != null) {
                    try {
                        out.close()
                    } catch (Exception ex) {}
                }
            }
        }

        flash.message = 'message.exported'
        flash.args = ["${recordCount}", "${fileCount}"]
        flash.defaultMessage = "${recordCount} records were exported into ${fileCount} files"
        redirect(action: export)
    }

    def imports = {
        def names = []
        def path = servletContext.getRealPath('/')
        if (path) {
            def dir = new File(new File(path).getParent(), "grails-app${File.separator}i18n")
            if (dir.exists() && dir.canRead()) {
                def name
                dir.listFiles().each {
                    if (it.isFile() && it.canRead() && it.getName().endsWith('.properties')) {
                        name = it.getName()
                        names << name.substring(0, name.length() - 11)
                    }
                }

                names.sort()
            }
        }

        return [names: names]
    }

    def load = {
        def name = params.file
        if (name) {
            name += '.properties'
            def path = servletContext.getRealPath('/')
            if (path) {
                def dir = new File(new File(path).getParent(), "grails-app${File.separator}i18n")
                if (dir.exists() && dir.canRead()) {
                    def file = new File(dir, name)
                    if (file.isFile() && file.canRead()) {
                        def locale
                        if (name ==~ /.+_[a-z][a-z]_[A-Z][A-Z]\.properties$/) {
                            locale = new Locale(name.substring(name.length() - 16, name.length() - 14), name.substring(name.length() - 13, name.length() - 11))
                        } else if (name ==~ /.+_[a-z][a-z]\.properties$/) {
                            locale = new Locale(name.substring(name.length() - 13, name.length() - 11))
                        } else {
                            locale = null
                        }

                        def counts = DatabaseMessageSource.loadPropertyFile(file, locale, utilService.currentCompany())
                        if (counts.imported) utilService.cacheService.resetAll('message', utilService.currentCompany().securityCode)
                        flash.message = 'message.imports.counts'
                        flash.args = [counts.imported, counts.skipped]
                        flash.defaultMessage = "Imported ${counts.imported} key(s). Skipped ${counts.skipped} key(s)."
                    } else {
                        flash.message = 'message.imports.access'
                        flash.args = [file]
                        flash.defaultMessage = "Unable to access ${file}"
                    }
                } else {
                    flash.message = 'message.imports.access'
                    flash.args = [dir]
                    flash.defaultMessage = "Unable to access ${dir}"
                }
            } else {
                flash.message = 'message.imports.access'
                flash.args = ['/']
                flash.defaultMessage = 'Unable to access /'
            }
        } else {
            flash.message = 'message.imports.missing'
            flash.defaultMessage = 'No properties file selected'
        }

        redirect(action: 'imports')
    }

// --------------------------------------------- Support Methods ---------------------------------------------

    private localeIsValid(locale) {
        if (locale) {
            if (locale == '*') return true

            if (locale.length() == 2) {
                return (SystemLanguage.countByCode(locale) == 1)
            }

            if (locale.length() == 4) {
                return (SystemLanguage.countByCode(locale.substring(0, 2)) == 1 && SystemCountry.countByCode(locale.substring(2)) == 1)
            }
        }

        return false
    }
}