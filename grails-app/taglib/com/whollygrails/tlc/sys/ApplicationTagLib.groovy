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
package com.whollygrails.tlc.sys

import java.util.concurrent.atomic.AtomicInteger
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.ConfigurationHolder

class ApplicationTagLib {

    private static final AtomicInteger helpCount = new AtomicInteger(0)

    def utilService

    // Outputs the current user name, HTML encoded
    def userName = {
        if (utilService.currentUser()) out << utilService.currentUser().name.encodeAsHTML()
    }

    // Outputs the current company name, HTML encoded
    def companyName = {
        if (utilService.currentCompany()) out << utilService.currentCompany().name.encodeAsHTML()
    }

    // Outputs the current company's logo image file name (e.g. myLogo.png)
    def companyLogo = {
        out << utilService.currentLogo()
    }

    // Make the body visible only to users with the specified activity(ies)
    // This tag cannot be nested
    def permit = {attrs, body ->
        def activities = (attrs.activity) ? attrs.activity.split(",")*.trim() : []
        for (acty in activities) {
            if (utilService.permitted(acty)) {
                out << body()
                return
            }
        }
    }

    // Outputs a new line character (used in text emails)
    def newLine = {
        out << '\n'
    }

    // Wrapper around the standard message tag that ensures HTML encoding. Does
    // not support the 'internal-use' error argument.
    def msg = {attrs, body ->
        out << message(code: attrs.code, args: attrs.args, default: attrs.default, encodeAs: 'HTML')
    }

    // Replaces the standard Grails renderErrors tag
    def listErrors = {attrs, body ->
        def bean = attrs.bean
        if (bean) {
            def errors = bean.errors?.allErrors
            if (errors) {
                out << '<ul>'

                errors.each {error ->
                    out << '<li>'
                    out << utilService.errorMessage(error).encodeAsHTML()
                    out << '</li>'
                }

                out << '</ul>'
            }
        }
    }

    // Sets up the help ballons system in the header of a page (or template)
    def helpSetup = {attrs, body ->
        def icon = attrs['icon'] ?: g.resource(dir: 'images', file: 'balloon-icon.gif')
        def button = attrs['button'] ?: g.resource(dir: 'images', file: 'balloon-button.png')
        def prefix = attrs['balloonPrefix'] ?: g.resource(dir: 'images') + '/balloon-'

        out << """<script type="text/javascript">
  HelpBalloon.Options.prototype = Object.extend(HelpBalloon.Options.prototype, {
    icon: '$icon',
    button: '$button',
    balloonPrefix: '$prefix'
  });
</script>
"""
    }

    // Outputs the HTML for a help button. Note that, unlike the plugin, this
    // version automatically HTML encodes everything
    def help = {attrs, body ->
        if (!utilService.currentUser()?.disableHelp) {
            def title = attrs['title']
            def content = attrs['content']
            def code = attrs['code']
            def suffix = attrs['suffix'] ?: '.help'

            if (!title && code) title = g.message(code: code)
            if (!content && code) content = g.message(code: code + suffix)

            title = title ?: ''
            content = content ?: ''
            title = title.encodeAsHTML()
            content = content.encodeAsHTML()

            out << """<script type="text/javascript">
  var hb${helpCount.incrementAndGet()} = new HelpBalloon({
    title: '${title.encodeAsJavaScript()}',
    content: '${content.encodeAsJavaScript()}'
  });
</script>
"""
        }
    }

    // Special tag for displaying the current user profile settings and date/time and number formatting
    def formatHelp = {attrs, body ->
        def now = new Date()
        def num = 1234567.89
        def locale = utilService.currentLocale()
        def title = g.msg(code: 'formats.title', default: 'Data Entry Formats')
        def content = """
<table style="margin-top:8px;">
  <tr>
    <td>${g.msg(code: 'formats.date', default: 'Date')}:</td>
    <td>${utilService.format(now, 1).encodeAsHTML()}</td>
  </tr>
  <tr>
    <td>${g.msg(code: 'formats.date.and.time', default: 'Date+Time')}:</td>
    <td>${utilService.format(now, 2).encodeAsHTML()}</td>
  </tr>
  <tr>
    <td>${g.msg(code: 'formats.positive.number', default: 'Positive')}:</td>
    <td>${utilService.format(num, 2).encodeAsHTML()}</td>
  </tr>
  <tr>
    <td>${g.msg(code: 'formats.negative.number', default: 'Negative')}:</td>
    <td>${utilService.format(-num, 2).encodeAsHTML()}</td>
  </tr>
</table>
<div align="center" style="margin-top:8px;"><img src="${resource(dir: 'images/flags', file: utilService.currentFlag() + '.gif')}"/><br/>
<p style="margin-top:3px;">${g.msg(code: 'formats.browser', args: [locale.getLanguage(), locale.getCountry()], default: "Your current locale is ${locale.getLanguage()}${locale.getCountry()}")}</p></div>
        """
        out << """<script type="text/javascript">
  var hb${helpCount.incrementAndGet()} = new HelpBalloon({
    title: '${title.encodeAsJavaScript()}',
    content: '${content.encodeAsJavaScript()}'
  });
</script>
"""
    }

    // Outputs a page tile as an '<h1>' or, if page help text is available, as a link to popup that help
    def pageTitle = {attrs, body ->
        if (attrs.code) {
            def code = attrs.help ?: attrs.code
            if (utilService.hasPageHelp(code)) {
                out << '<div class=pageHelpTitle><a href="javascript:openPageHelp(\''
                out << createLink(controller: 'systemPageHelp', action: 'display')
                out << '?code='
                out << code.encodeAsURL()
                out << '\')">'
                out << msg(code: attrs.code, args: attrs.args, default: attrs.default)
                out << '</a>'
                if (attrs.returns?.equalsIgnoreCase('true')) out << ' ' + drilldownReturn(params: attrs.params)
                out << '</div>'
            } else {
                out << '<h1>'
                out << msg(code: attrs.code, args: attrs.args, default: attrs.default)
                if (attrs.returns?.equalsIgnoreCase('true')) out << ' ' + drilldownReturn(params: attrs.params)
                out << '</h1>'
            }
        }
    }

    // Replaces the standard Grails fieldValue tag and can also handle just a given value. If
    // both bean/field and value are specified, the bean/field specification takes precedence
    def display = {attrs, body ->
        def bean = attrs.bean
        def field = attrs.field
        def value = attrs.value
        def scale = attrs.scale
        if (scale && scale instanceof String) scale = scale.toInteger()
        def grouped = attrs.grouped
        if (grouped && grouped instanceof String) grouped = grouped.toBoolean()
        if (bean && field) value = bean.errors?.getFieldError(field)?.rejectedValue ?: bean."$field"

        // Handle special case of boolean where we want to output an <img> tag
        if (value != null && value instanceof Boolean) {
            out << '<img src="'
            out << resource(dir: 'images', file: "${value}.png")
            out << '" border="0"/>'
        } else {
            out << utilService.format(value, scale, grouped, attrs.locale).encodeAsHTML()
        }
    }

    // Output a drilldown tag
    def drilldown = {attrs, body ->
        def map = [:]
        map.ddController = attrs.ddController ?: params.controller
        map.ddAction = attrs.ddAction ?: params.action
        map.ddValue = attrs.value
        def action = attrs.action ?: 'list'
        if (attrs.domain) map.ddDomain = attrs.domain
        if (params.max) map.ddMax = params.max
        if (params.offset) map.ddOffset = params.offset
        if (params.sort) map.ddSort = params.sort
        if (params.order) map.ddOrder = params.order
        if (attrs.params) map.putAll(attrs.params)

        out << g.link(controller: attrs.controller, action: action, params: map) {
            setLink(attrs, 'drilldown.png')
        }
    }

    // Output a drilldown return tag
    def drilldownReturn = {attrs, body ->
        def entry = utilService.drilldownService.entry(session, params, null)
        if (entry) {
            def map = [:]
            if (entry.srcMax) map.max = entry.srcMax
            if (entry.srcOffset) map.offset = entry.srcOffset
            if (entry.srcSort) map.sort = entry.srcSort
            if (entry.srcOrder) map.order = entry.srcOrder
            if (attrs.params) map.putAll(attrs.params)
            out << g.link(controller: entry.srcController, action: entry.srcAction, params: map) {
                setLink(attrs, 'drillup.png')
            }
        }
    }

    // Reset the drilldown stack
    def drilldownReset = {attrs, body ->
        utilService.drilldownService.reset(session)
    }

    // Output the HTML for the criteria form
    def criteria = {attrs, body ->
        def options = [:]
        if (attrs.domain) options.domain = attrs.domain[0].toUpperCase() + attrs.domain[1..-1]
        def specifiedFields = [:]
        if (attrs.include) {
            def list = attrs.include.split(",")*.trim()
            for (int i = 0; i < list.size(); i++) {

                // If it's a translated field with an underlying value such as a code field
                if (list[i].endsWith('*')) {
                    list[i] = list[i].substring(0, list[i].length() - 1).trim()
                    specifiedFields.put(list[i], true)  // Note it was markes as being a translation
                } else {
                    specifiedFields.put(list[i], false) // Not a translation
                }
            }

            if (!utilService.currentUser().administrator) options.include = list
        }

        // The following line has been disabled since the bookkeeping system only uses the include attribute
        // if (attrs.exclude) options.exclude = attrs.exclude.split(",")*.trim()
        def props = utilService.criteriaService.getDomainProperties(params, options)
        def verification = ''
        if (props) {
            def specified
            props.each {
                verification += (verification) ? ",${it.name}" : it.name
                it.display = g.msg(code: it.code, default: it.default)
                specified = specifiedFields.get(it.name)
                if (specified == null) {
                    it.display += ' +'  // A field that only the system administrator sees
                } else if (specified) {
                    it.display += ' *'  // A field that the user sees and was marked as being a translation
                }
            }

            while (verification.length() < 50) {
                verification += "${CacheService.IMPOSSIBLE_VALUE}${params.controller}${params.action}";
            }

            props.sort {it.display.toString()}

            def url = g.link(controller: "criteria", action: "apply")
            url = url.substring(url.indexOf('"') + 1, url.lastIndexOf('"'))

            out << '<form action="' + url + '" method="post">\n'
            out << '  <input type="hidden" name="crController" value="' + params.controller + '"/>\n'
            out << '  <input type="hidden" name="crAction" value="' + params.action + '"/>\n'
            out << '  <input type="hidden" name="crDomain" value="' + utilService.criteriaService.getDomain(params, options) + '"/>\n'
            out << '  <input type="hidden" name="crLinkage" value="' + Obfusticator.encrypt("${params.controller}.${params.action}", verification) + '"/>\n'
            if (params.max) out << '  <input type="hidden" name="max" value="' + params.max + '"/>\n'
            if (params.offset) out << '  <input type="hidden" name="offset" value="' + params.offset + '"/>\n'
            if (params.sort) out << '  <input type="hidden" name="sort" value="' + params.sort + '"/>\n'
            if (params.order) out << '  <input type="hidden" name="order" value="' + params.order + '"/>\n'
            out << '  ' + g.msg(code: "criteria.criteria", default: "Criteria") + ':\n'
            out << '  <select name="property">\n'

            def map = utilService.criteriaService.getCriteria(session, params)
            out << '    <option value="none"' + (!map ? " selected" : "") + '>' + g.msg(code: "generic.no.selection", default: "-- none --") + '</option>\n'

            props.each {
                out << '    <option value="' + it.name + '"' + ((map?.property == it.name) ? " selected" : "") + '>' + it.display + '</option>\n'
            }

            out << '  </select>\n'
            out << '  <select name="test">\n'
            out << '    <option value="none"' + (!map ? " selected" : "") + '>' + g.msg(code: "generic.no.selection", default: "-- none --") + '</option>\n'
            out << '    <option value="equal"' + ((map?.test == "equal") ? " selected" : "") + '>' + g.msg(code: "criteria.test.equal", default: "Equal to") + '</option>\n'
            out << '    <option value="not.equal"' + ((map?.test == "not.equal") ? " selected" : "") + '>' + g.msg(code: "criteria.test.not.equal", default: "Not equal to") + '</option>\n'
            out << '    <option value="null"' + ((map?.test == "null") ? " selected" : "") + '>' + g.msg(code: "criteria.test.null", default: "Is null") + '</option>\n'
            out << '    <option value="not.null"' + ((map?.test == "not.null") ? " selected" : "") + '>' + g.msg(code: "criteria.test.not.null", default: "Is not null") + '</option>\n'
            out << '    <option value="less"' + ((map?.test == "less") ? " selected" : "") + '>' + g.msg(code: "criteria.test.less", default: "Less than") + '</option>\n'
            out << '    <option value="less.or.equal"' + ((map?.test == "less.or.equal") ? " selected" : "") + '>' + g.msg(code: "criteria.test.less.or.equal", default: "Less than or equal to") + '</option>\n'
            out << '    <option value="greater"' + ((map?.test == "greater") ? " selected" : "") + '>' + g.msg(code: "criteria.test.greater", default: "Greater than") + '</option>\n'
            out << '    <option value="greater.or.equal"' + ((map?.test == "greater.or.equal") ? " selected" : "") + '>' + g.msg(code: "criteria.test.greater.or.equal", default: "Greater than or equal to") + '</option>\n'
            out << '    <option value="like"' + ((map?.test == "like") ? " selected" : "") + '>' + g.msg(code: "criteria.test.like", default: "Like") + '</option>\n'
            out << '    <option value="not.like"' + ((map?.test == "not.like") ? " selected" : "") + '>' + g.msg(code: "criteria.test.not.like", default: "Not like") + '</option>\n'
            out << '    <option value="between"' + ((map?.test == "between") ? " selected" : "") + '>' + g.msg(code: "criteria.test.between", default: "Between") + '</option>\n'
            out << '    <option value="not.between"' + ((map?.test == "not.between") ? " selected" : "") + '>' + g.msg(code: "criteria.test.not.between", default: "Not between") + '</option>\n'
            out << '    <option value="in"' + ((map?.test == "in") ? " selected" : "") + '>' + g.msg(code: "criteria.test.in", default: "In") + '</option>\n'
            out << '    <option value="not.in"' + ((map?.test == "not.in") ? " selected" : "") + '>' + g.msg(code: "criteria.test.not.in", default: "Not in") + '</option>\n'
            out << '  </select>\n'
            out << '  <input type="text" name="value" value="' + (map?.value ? map.value.encodeAsHTML() : "") + '"/>\n'
            out << '  <input class="apply" type="submit" value="' + g.msg(code: "criteria.apply", default: "Apply") + '"/>\n'
            out << '  ' + g.help(code: "criteria.criteria")
            out << '</form>\n'
        }
    }

    // Reset all criteria
    def criteriaReset = {attrs, body ->
        utilService.criteriaService.reset(session)
    }

    // Reset all filters
    def filterReset = {attrs, body ->
        utilService.resetFilters()
    }

    // Format a value for display
    def format = {attrs, body ->
        def scale = attrs.scale
        def grouped = attrs.grouped
        if (scale && scale instanceof String) scale = scale.toInteger()
        if (grouped && grouped instanceof String) grouped = grouped.toBoolean()

        out << utilService.format(attrs.value, scale, grouped, attrs.locale).encodeAsHTML()
    }

    // Display the title of a menu
    def menuTitle = {attrs, body ->
        def option = utilService.menuService.currentMenuOption(request, session, params)
        if (option) {

            // If they want a special 'sub-menu' title
            if (option.parameters) {
                def dflt = g.msg(code: 'menu.option.' + option.path, default: (option.parameters ?: option.title))
                out << g.msg(code: 'menu.submenu.' + option.path, default: dflt)
            } else {    // Just use the ordinary option title
                out << g.msg(code: 'menu.option.' + option.path, default: option.title)
            }
        } else {    // Need the main menu title
            if (!attrs.default) attrs.default = 'Main Menu'
            out << g.msg(code: 'menu.main', default: attrs.default)
        }
    }

    // Display the crumbs on a menu page
    def menuCrumbs = {attrs, body ->
        def nodes = utilService.menuService.listMenuCrumbs(utilService.cacheService, request, session, params)
        def single = (attrs.single && attrs.single.equalsIgnoreCase('true')) ? true : false
        if (!single && nodes.size() == 0) return
        if (!attrs.default) attrs.default = 'Main'

        if (nodes.size() == 0) {
            if (single) {
                out << '<span class="crumb">' + g.msg(code: 'menu.crumb', default: attrs.default) + '</span>'
            }
        } else {
            out << g.link(action: 'display', id: '0') {
                '<span class="crumb">' + g.msg(code: 'menu.crumb', default: attrs.default) + '</span>'
            }

            def path, crumb, dflt
            def img = '&nbsp;' +
                    (attrs.image ? '<img src="' + attrs.image + '" border="0"/>' : '<img src="' + g.resource(dir: 'images', file: 'crumb.png') + '" border="0"/>') +
                    '&nbsp;'
            for (int i = 0; i < nodes.size(); i++) {
                out << img
                path = nodes[i].path
                dflt = path.contains('.') ? path.substring(path.lastIndexOf('.') + 1) : path
                crumb = '<span class="crumb">' + g.msg(code: "menu.crumb.${path}", default: dflt) + '</span>'
                if (i == nodes.size() - 1) {
                    out << crumb
                } else {
                    out << g.link(action: 'display', id: "${nodes[i].id}") {crumb}
                }
            }
        }
    }

    // Display the crumbs on an option page
    def optionCrumbs = {attrs, body ->
        def nodes = utilService.menuService.listOptionCrumbs(utilService.cacheService, request, session)
        if (nodes.size() == 0) return
        def active = (attrs.active && attrs.active.equalsIgnoreCase('false')) ? false : true
        if (!attrs.default) attrs.default = 'Main'

        out << g.link(controller: 'systemMenu', action: 'display', id: '0') {
            '<span class="crumb">' + g.msg(code: 'menu.crumb', default: attrs.default) + '</span>'
        }

        def path, crumb, dflt
        def img = '&nbsp;' +
                (attrs.image ? '<img src="' + attrs.image + '" border="0"/>' : '<img src="' + g.resource(dir: 'images', file: 'crumb.png') + '" border="0"/>') +
                '&nbsp;'
        for (int i = 0; i < nodes.size(); i++) {
            out << img
            path = nodes[i].path
            dflt = path.contains('.') ? path.substring(path.lastIndexOf('.') + 1) : path
            crumb = '<span class="crumb">' + g.msg(code: "menu.crumb.${path}", default: dflt) + '</span>'
            if (i == nodes.size() - 1) {
                if (active) {
                    out << g.link(controller: 'systemMenu', action: 'execute', id: "${nodes[i].id}") {crumb}
                } else {
                    out << crumb
                }
            } else {
                out << g.link(controller: 'systemMenu', action: 'display', id: "${nodes[i].id}") {crumb}
            }
        }
    }

    // Reset the menu to 'main' and also clear any drilldown stack and criteria
    def menuReset = {attrs, body ->
        utilService.menuService.reset(session)
        g.drilldownReset()
        g.criteriaReset()
    }

    // An alternative to the standard Grails select list for use with domain objects (i.e. not suitable for strings, integers etc
    // but can be used with maps or expandos that have the appearnce of a domain object) that allows for translations & multiple
    // display fields etc. Unrecognized attributes are passed through to the HTML tag. If either one of 'name' or 'id' is supplied,
    // but the other is not supplied, the 'name' and 'id' will be set to the same value based on which ever one is supplied. One
    // or other of them MUST be supplied. The recognized attributes are as follows:
    //
    //      options     Required. The list of options to select from.
    //      selected    Optional. If a List (even an empty one) this will be a multiple select otherwise it will be a single select.
    //      returns     Optional. The name of the property to be returned. Also used for testing selected values against option values.
    //                            Defaults to 'id'
    //      displays    Optional. The property (or properties, if a List) to be displayed in the selection list. Defaults to the 'returns'
    //                            property unless a 'code' is specified in which case the 'default' property of the code will be used or
    //                            the 'code' property itself when there is no default.
    //      prefix      Optional. If a mesage lookup is to be performed, the prefix to which a dot and the 'code' will be added. If there
    //                            is no 'code' attribute then the translation will be done on the 'displays' property (or the first one in
    //                            the list if 'displays' is a List.)
    //      code        Optional. The name of the property whose value is to be appended to the end of the 'prefix' for a message lookup.
    //                            If a 'default' property is specified and the default property is not included in the 'displays' attribute,
    //                            the 'default' property will be appended to the 'displays' list otherwise, if no 'default' property is
    //                            specified and the 'code' property is not in the 'displays' list, the 'code' property will be appended to
    //                            the 'displays' list instead.
    //      default     Optional. The name of the property to be used as a default if a code translation is requested but the code
    //                            cannot be found.
    //      delimiter   Optional. If 'displays' is a List, the String to use to separate properties being displayed. Defaults to ' - '
    //      noSelection Optional. Same as in the standard Grails select tag
    //      sort        Optional. If set to 'false', no sorting will be done. If set to a property name, will sort by that property.
    //                            Defaults to sorting by the result of evaluating the 'displays' argument
    //
    // The list displayed to the user is automatically sorted by the property(ies) being show to them. Consequently, the caller need not
    // have previously specified any sorting themselves (such as in the database select statement).
    def domainSelect = {attrs, body ->

        // Grab the recognized attributes
        def options = attrs.remove('options')
        def selected = attrs.remove('selected')
        def returns = attrs.remove('returns') ?: 'id'
        def displays = attrs.remove('displays')
        def prefix = attrs.remove('prefix') ?: ''
        def code = attrs.remove('code')
        def dflt = attrs.remove('default')
        def delimiter = attrs.remove('delimiter') ?: ' - '
        def noSelection = attrs.remove('noSelection')
        def name = attrs.remove('name')
        def id = attrs.remove('id')
        def sort = attrs.remove('sort') ?: ''

        // See if we have enough info to proceed
        if (name || id) {

            // Make name and id the same if one of them is not specified
            if (name && !id) {
                id = name
            } else if (id && !name) {
                name = id
            }

            // Clean up any disabled non-setting
            if (!attrs.disabled || attrs.disabled.toString().equalsIgnoreCase('false')) attrs.remove('disabled')

            // Determine if this is a multiple selection and ensure that
            // 'selected' is always a List - even if its only a single item
            def multiple = false
            if (selected == null) {
                selected = []
            } else if (selected instanceof List) {
                multiple = true
            } else {
                selected = [selected]
            }

            // If there are options available
            if (options) {

                // Note whether the list is to be sorted or not
                def toBeSorted = !sort.equalsIgnoreCase('false')

                def selectedIsPrimitive = (selected && (selected[0] instanceof String || selected[0] instanceof Number || selected[0] instanceof Date || selected[0] instanceof Boolean))

                // Ensure the display attribute is in the form of a list
                if (displays) {
                    if (displays instanceof String) displays = [displays]
                } else {    // Ensure there is something to display

                    // Try, first of all, to display for the default for any code lookup
                    if (dflt) {
                        displays = [dflt]
                    } else if (code) { // No default, so if they specified a code property, display that
                        displays = [code]
                    } else {    // Last resort is to display the 'returns' property
                        displays = [returns]
                    }
                }

                // If there's a prefix then clean it up and ensure there's a code
                if (prefix) {

                    // Add a dot to the prefix if required
                    if (!prefix.endsWith('.')) prefix += '.'

                    // If no code, assume its the first 'displays' entry
                    if (!code) code = displays[0]
                }

                // If a lookup is required
                if (code) {

                    // Ensure we have a default property
                    if (!dflt) dflt = code

                    // Ensure the default is in the display list
                    if (!displays.contains(dflt)) displays << dflt
                }

                // Work through the options creating the selection list
                def list = []
                for (option in options) {

                    // Create the map element and add in the return value for this option
                    def map = [:]
                    map.returns = option."${returns}"

                    // Work through the display properties creating the text shown in the list
                    def text = ''
                    for (display in displays) {

                        // Add the delimiter if not FTT
                        if (text) text += delimiter

                        // Perform a message lookup if required
                        if (display == dflt) {
                            def codeval = option."${code}"
                            def dfltval = option."${dflt}"
                            text += message(code: "${prefix}${codeval}", default: "${dfltval}")
                        } else {    // Just an ordinary property, so use its value
                            def propval = option."${display}"
                            text += "${propval}"
                        }
                    }

                    // Store the text to be displayed
                    map.displays = text

                    // If they want a sort, store the sort value
                    if (toBeSorted) map.sort = sort ? option."${sort}" : text

                    // See if it is one of the selected options
                    if (selectedIsPrimitive) {
                        if (selected.find {map.returns == it}) map.selected = true
                    } else {
                        if (selected.find {map.returns == it."${returns}"}) map.selected = true
                    }

                    // Put the map in the list
                    list << map
                }

                // Sort the list if required
                if (toBeSorted) list.sort {it.sort}

                // Now do the output
                out << '<select name="' + name.encodeAsHTML() + '" id="' + id.encodeAsHTML() + '" '

                // Pass through any attributes we didn't recognize
                attrs.each {
                    out << it.key.encodeAsHTML() + '="' + it.value.encodeAsHTML() + '" '
                }

                // Specify if it's a multiple selection list
                if (multiple) out << 'multiple="multiple" '

                // End of opening tag
                out << '>\n'

                // Output any 'no selection' option
                if (noSelection) {
                    def key, value
                    noSelection.each {
                        key = it.key
                        value = it.value
                    }

                    out << '<option value="' + "${key}".encodeAsHTML() + '" >' + "${value}".encodeAsHTML() + '</option>\n'
                }

                // Output the real options
                for (item in list) {
                    out << '<option value="' + "${item.returns}".encodeAsHTML() + '" '
                    if (item.selected) out << 'selected="selected" '
                    out << '>' + "${item.displays}".encodeAsHTML() + '</option>\n'
                }

                // Finish off the select tag
                out << '</select>\n'
            } else if (noSelection) {   // Only a no-selection option to display
                out << '<select name="' + name.encodeAsHTML() + '" id="' + id.encodeAsHTML() + '" '

                // Pass through any attributes we didn't recognize
                attrs.each {
                    out << it.key.encodeAsHTML() + '="' + it.value.encodeAsHTML() + '" '
                }

                // Specify if it's a multiple selection list
                if (multiple) out << 'multiple="multiple" '

                // End of opening tag
                out << '>\n'

                // Output the 'no selection' option
                def key, value
                noSelection.each {
                    key = it.key
                    value = it.value
                }

                out << '<option value="' + "${key}".encodeAsHTML() + '" >' + "${value}".encodeAsHTML() + '</option>\n'

                // Finish off the select tag
                out << '</select>\n'
            }
        }
    }

    // This is purely for the index page of the Wholly Grails web site.
    // It returns the mailto: href for the person defined in config.groovy
    // as the grails.mail.username. The href is returned with all characters
    // reversed and dots, colons and at-signs messed up. All this fooling
    // around is to help prevent spammers using web page scrapers.
    def revcon = {attrs, body ->
        def user = 'mailto:' + ConfigurationHolder.config.grails.mail.username
        user = user.replace('.', '..')
        user = user.replace(':', ']')
        user = user.replace('@', '[')
        out << user.reverse()
    }

    // Returns the current application version
    def appVersion = {attrs, body ->
        out << ApplicationHolder.getApplication().getMetadata().get('app.version')
    }

    // --------------------------------------------- Support Methods ---------------------------------------------

    private setLink(attrs, png) {
        def txt = attrs.text ?: ''
        if (txt) txt = txt.encodeAsHTML()

        def img = attrs.image ?: ''
        if (txt) {
            if (img.toLowerCase() == 'false') {
                img = ''
            } else if (img.toLowerCase() == 'true' || (!img && attrs.imageAfter)) {
                img = '<img src="' + g.resource(dir: 'images', file: png) + '" border="0"/>'
            } else if (img) {
                img = '<img src="' + img + '" border="0"/>'
            }
        } else {  // No text
            if (!img || img.toLowerCase() == 'true' || img.toLowerCase() == 'false') {
                img = '<img src="' + g.resource(dir: 'images', file: png) + '" border="0"/>'
            } else {
                img = '<img src="' + img + '" border="0"/>'
            }
        }

        if (img && txt) {
            if (attrs.imageAfter && attrs.imageAfter.toLowerCase() == 'false') {
                txt = img + '&nbsp;' + txt
            } else {
                txt = txt + '&nbsp;' + img
            }
        } else if (img) {
            txt = img
        }

        return txt
    }
}
