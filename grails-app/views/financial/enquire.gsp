<%--
 ~  Copyright 2010 Wholly Grails.
 ~
 ~  This file is part of the Three Ledger Core (TLC) software
 ~  from Wholly Grails.
 ~
 ~  TLC is free software: you can redistribute it and/or modify
 ~  it under the terms of the GNU General Public License as published by
 ~  the Free Software Foundation, either version 3 of the License, or
 ~  (at your option) any later version.
 ~
 ~  TLC is distributed in the hope that it will be useful,
 ~  but WITHOUT ANY WARRANTY; without even the implied warranty of
 ~  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 ~  GNU General Public License for more details.
 ~
 ~  You should have received a copy of the GNU General Public License
 ~  along with TLC.  If not, see <http://www.gnu.org/licenses/>.
 --%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title><g:msg code="financial.journal.enquire" default="Financial Journal Enquiry"/></title>
</head>
<body>
<div class="nav">
    <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:msg code="home" default="Home"/></a></span>
    <span class="menuButton"><g:link class="menu" controller="systemMenu" action="display"><g:msg code="systemMenu.display" default="Menu"/></g:link></span>
</div>
<div class="body">
    <g:pageTitle code="financial.journal.enquire" default="Financial Journal Enquiry" help="document.enquire"/>
    <g:if test="${flash.message}">
        <div class="message"><g:msg code="${flash.message}" args="${flash.args}" default="${flash.defaultMessage}"/></div>
    </g:if>
    <g:hasErrors bean="${documentInstance}">
        <div class="errors">
            <g:listErrors bean="${documentInstance}"/>
        </div>
    </g:hasErrors>
    <g:form action="enquire" method="post">
        <div class="dialog">
            <table>
                <tbody>
                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="type.id"><g:msg code="document.enquire.type" default="Document Type"/>:</label>
                    </td>
                    <td valign="top" class="value nowrap ${hasErrors(bean: documentInstance, field: 'type', 'errors')}">
                        <g:domainSelect initialField="true" name="type.id" options="${documentTypeList}" selected="${documentInstance?.type}" displays="${['code', 'name']}"/>&nbsp;<g:help code="document.enquire.type"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="code"><g:msg code="document.enquire.code" default="Code"/>:</label>
                    </td>
                    <td valign="top" class="value nowrap ${hasErrors(bean: documentInstance, field: 'code', 'errors')}">
                        <input type="text" maxlength="10" size="10" id="code" name="code" value="${display(bean: documentInstance, field: 'code')}"/>&nbsp;<g:help code="document.enquire.code"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="displayCurrency"><g:msg code="generic.displayCurrency" default="Display Currency"/>:</label>
                    </td>
                    <td valign="top" class="value nowrap">
                        <g:domainSelect class="${displayCurrencyClass}" name="displayCurrency" options="${currencyList}" selected="${displayCurrency}" prefix="currency.name" code="code" default="name" noSelection="['null': msg(code: 'generic.no.selection', default: '-- none --')]"/>&nbsp;<g:help code="generic.displayCurrency"/>
                    </td>

                    <td><span class="button"><input class="save" type="submit" value="${msg(code: 'generic.enquire', 'default': 'Enquire')}"/></span></td>
                    <td></td>
                </tr>
                <g:if test="${documentInstance.id}">
                    <tr class="prop">
                        <td valign="top" class="name"><g:msg code="document.description" default="Description"/>:</td>
                        <td valign="top" class="value" colspan="2">${display(bean: documentInstance, field: 'description')}</td>

                        <td valign="top" class="name"><g:msg code="generalTransaction.adjustment" default="Adjustment"/>:&nbsp;&nbsp;${display(bean: documentInstance, field: 'sourceAdjustment')}</td>

                        <td valign="top" class="name"><g:msg code="document.currency.text" default="Document Currency"/>:</td>
                        <td valign="top" class="value">${msg(code: 'currency.name.' + documentInstance.currency.code, default: documentInstance.currency.name)}</td>

                        <td valign="top" class="name"><g:msg code="account.enquire.period" default="Period"/>:</td>
                        <td valign="top" class="value nowrap">${documentInstance.period.code.encodeAsHTML()}</td>
                    </tr>
                    <tr class="prop">
                        <td valign="top" class="name"><g:msg code="generic.documentDate" default="Document Date"/>:</td>
                        <td valign="top" class="value">${display(bean: documentInstance, field: 'documentDate', scale: 1)}</td>

                        <td></td>
                        <td></td>

                        <td valign="top" class="name"><g:msg code="document.customer.reference" default="Reference"/>:</td>
                        <td valign="top" class="value">${display(bean: documentInstance, field: 'reference')}</td>

                        <td valign="top" class="name"><g:msg code="document.posted" default="Posted"/>:</td>
                        <td valign="top" class="value nowrap">${display(bean: documentInstance, field: 'dateCreated', scale: 1)}</td>
                    </tr>
                </g:if>
                </tbody>
            </table>
        </div>
    </g:form>
    <g:if test="${documentInstance.id}">
        <div class="list">
            <table>
                <thead>
                <tr>
                    <th><g:msg code="document.line.accountCode" default="Account"/></th>
                    <th><g:msg code="account.name" default="Name"/></th>
                    <th><g:msg code="document.line.description" default="Description"/></th>
                    <th class="nowrap center"><g:msg code="document.line.to" default="T/O"/></th>
                    <th class="right"><g:msg code="generic.debit" default="Debit"/></th>
                    <th class="right"><g:msg code="generic.credit" default="Credit"/></th>
                </tr>
                </thead>
                <tbody>
                <g:each in="${documentInstance.lines}" status="i" var="lineInstance">
                    <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
                        <td><g:enquiryLink target="${((lineInstance.customer ?: lineInstance.supplier) ?: lineInstance.balance.account)}" displayPeriod="${documentInstance.period}" displayCurrency="${displayCurrency}">${display(bean: lineInstance, field: 'accountCode')}</g:enquiryLink></td>
                        <td>${display(bean: lineInstance, field: 'accountName')}</td>
                        <td>${display(bean: lineInstance, field: 'description')}</td>
                        <td class="center"><g:if test="${lineInstance.accountType != 'gl'}">${display(bean: lineInstance, field: 'affectsTurnover')}</g:if></td>
                        <td class="right"><g:debit context="${documentInstance}" line="${lineInstance}" field="value" currency="${displayCurrency}"/></td>
                        <td class="right"><g:credit context="${documentInstance}" line="${lineInstance}" field="value" currency="${displayCurrency}"/></td>
                    </tr>
                </g:each>
                <tr>
                    <th></th>
                    <th></th>
                    <th class="right" colspan="2"><g:msg code="document.sourceTotals" default="Totals"/>:</th>
                    <th class="right"><g:format value="${totalInstance.debit}" scale="${totalInstance.scale}"/></th>
                    <th class="right"><g:format value="${totalInstance.credit}" scale="${totalInstance.scale}"/></th>
                </tr>
                </tbody>
            </table>
        </div>
    </g:if>
</div>
</body>
</html>
