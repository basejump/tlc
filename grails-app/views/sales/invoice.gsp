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
    <title><g:msg code="sales.invoice.entry" default="Sales Invoice/Cr. Note Entry"/></title>
    <g:yuiResources require="connection"/>
</head>
<body>
<div class="nav">
    <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:msg code="home" default="Home"/></a></span>
    <span class="menuButton"><g:link class="menu" controller="systemMenu" action="display"><g:msg code="systemMenu.display" default="Menu"/></g:link></span>
    <span class="menuButton"><g:link class="list" controller="document" action="templates" params="${[ctrl: 'sales', types: 'SI,SC']}"><g:msg code="templateDocument.load" default="Load Template"/></g:link></span>
</div>
<div class="body">
    <g:pageTitle code="sales.invoice.entry" default="Sales Invoice/Cr. Note Entry"/>
    <g:if test="${flash.message}">
        <div class="message"><g:msg code="${flash.message}" args="${flash.args}" default="${flash.defaultMessage}"/></div>
    </g:if>
    <g:hasErrors bean="${documentInstance}">
        <div class="errors">
            <g:listErrors bean="${documentInstance}"/>
        </div>
    </g:hasErrors>
    <div id="ajaxErrorMessage" class="errors" style="visibility:hidden;"></div>
    <g:form method="post">
        <input type="hidden" id="responseMessage" name="responseMessage" value="${msg(code: 'generic.ajax.response', default: 'Unable to understand the response from the server')}"/>
        <input type="hidden" id="timeoutMessage" name="timeoutMessage" value="${msg(code: 'generic.ajax.timeout', default: 'Operation timed out waiting for a response from the server')}"/>
        <input type="hidden" id="sourceNumber" name="sourceNumber" value="${display(bean: documentInstance, field: 'code')}"/>
        <div class="dialog">
            <table>
                <tbody>

                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="sourceCode"><g:msg code="document.customer" default="Customer"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'sourceCode', 'errors')}">
                        <input initialField="true" onchange="getLedger(this, '${createLink(controller: 'document', action: 'customer')}')" type="text" maxlength="20" size="20" id="sourceCode" name="sourceCode" value="${display(bean: documentInstance, field: 'sourceCode')}"/>&nbsp;<g:help code="document.customer"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="sourceName"><g:msg code="document.sourceName" default="Name"/>:</label>
                    </td>
                    <td valign="top" class="small-value">
                        <input disabled="true" type="text" size="50" id="sourceName" name="sourceName" value="${display(bean: documentInstance, field: 'sourceName')}"/>
                    </td>

                    <td></td>
                    <td></td>
                </tr>

                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="type.id"><g:msg code="document.type" default="Document Type"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'type', 'errors')}">
                        <g:domainSelect onchange="typeChanged()" onblur="getCode(this, '${createLink(controller: 'document', action: 'code')}', 'reference')" name="type.id" options="${documentTypeList}" selected="${documentInstance?.type}" displays="${['code', 'name']}" noSelection="['null': msg(code: 'generic.select', default: '-- select --')]"/>&nbsp;<g:help code="document.type"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="code"><g:msg code="document.code" default="Code"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'code', 'errors')}">
                        <input ${settings.codeEdit ? '' : 'disabled="true"'} type="text" maxlength="10" size="10" id="code" name="code" value="${display(bean: documentInstance, field: 'code')}"/>&nbsp;<g:help code="document.code"/>
                    </td>

                    <td></td>
                    <td></td>
                </tr>

                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="reference"><g:msg code="document.customer.reference" default="Reference"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'reference', 'errors')}">
                        <input type="text" maxLength="30" size="30" id="reference" name="reference" value="${display(bean: documentInstance, field: 'reference')}"/>&nbsp;<g:help code="document.customer.reference"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="description"><g:msg code="document.description" default="Description"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'description', 'errors')}">
                        <input type="text" maxlength="50" size="45" id="description" name="description" value="${display(bean: documentInstance, field: 'description')}"/>&nbsp;<g:help code="document.description"/>
                    </td>

                    <td></td>
                    <td></td>
                </tr>

                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="documentDate"><g:msg code="document.documentDate" default="Document Date"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'documentDate', 'errors')}">
                        <input type="text" onchange="getPeriod(this, '${createLink(controller: 'document', action: 'period')}', 'customer')" size="20" id="documentDate" name="documentDate" value="${display(bean: documentInstance, field: 'documentDate', scale: 1)}"/>&nbsp;<g:help code="document.documentDate"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="dueDate"><g:msg code="document.dueDate" default="Due Date"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'dueDate', 'errors')}">
                        <input type="text" size="20" id="dueDate" name="dueDate" value="${display(bean: documentInstance, field: 'dueDate', scale: 1)}"/>&nbsp;<g:help code="document.dueDate"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="sourceHold"><g:msg code="document.sourceHold" default="Hold"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'sourceHold', 'errors')}">
                        <g:checkBox name="sourceHold" value="${documentInstance?.sourceHold}"></g:checkBox>&nbsp;<g:help code="document.sourceHold"/>
                    </td>
                </tr>

                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="period.id"><g:msg code="document.period" default="Accounting Period"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'period', 'errors')}">
                        <g:select optionKey="id" optionValue="code" from="${periodList}" name="period.id" value="${documentInstance?.period?.id}"/>&nbsp;<g:help code="document.period"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="currency"><g:msg code="document.currency" default="Currency"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'currency', 'errors')}">
                        <g:domainSelect name="currency.id" options="${currencyList}" selected="${documentInstance?.currency}" prefix="currency.name" code="code" default="name"/>&nbsp;<g:help code="document.currency"/>
                    </td>

                    <td></td>
                    <td></td>
                </tr>

                <tr class="prop">
                    <td valign="top" class="name">
                        <label for="sourceGoods"><g:msg code="document.sourceGoods" default="Goods"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'sourceGoods', 'errors')}">
                        <input type="text" size="15" id="sourceGoods" name="sourceGoods" value="${display(bean: documentInstance, field: 'sourceGoods', scale: settings.decimals)}"/>&nbsp;<g:help code="document.sourceGoods"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="sourceTax"><g:msg code="document.sourceTax" default="Tax"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'sourceTax', 'errors')}">
                        <input type="text" size="15" id="sourceTax" name="sourceTax" value="${display(bean: documentInstance, field: 'sourceTax', scale: settings.decimals)}"/>&nbsp;<g:help code="document.sourceTax"/>
                    </td>

                    <td valign="top" class="name">
                        <label for="sourceTotal"><g:msg code="document.sourceTotal" default="Total"/>:</label>
                    </td>
                    <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'sourceTotal', 'errors')}">
                        <input type="text" size="15" id="sourceTotal" name="sourceTotal" value="${display(bean: documentInstance, field: 'sourceTotal', scale: settings.decimals)}"/>&nbsp;<g:help code="document.sourceTotal"/>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
        <div class="list entry">
            <table>
                <thead>
                <tr>
                    <th><g:msg code="document.line.accountCode" default="Account"/>&nbsp;<g:help code="document.line.accountCode"/></th>
                    <th><g:msg code="document.line.description" default="Description"/>&nbsp;<g:help code="document.line.description"/></th>
                    <th><g:msg code="document.line.taxCode" default="Tax Code"/>&nbsp;<g:help code="document.line.taxCode"/></th>
                    <th><g:msg code="document.line.goods" default="Goods"/>&nbsp;<g:help code="document.line.goods"/></th>
                    <th><g:msg code="document.line.tax" default="Tax"/>&nbsp;<g:help code="document.line.tax"/></th>
                    <th><g:msg code="document.line.total" default="Total"/>&nbsp;<g:help code="document.line.total"/></th>
                </tr>
                </thead>
                <tbody>
                <g:each in="${documentInstance.lines}" status="i" var="documentLine">
                    <tr>
                        <td class="narrow ${hasErrors(bean: documentLine, field: 'accountCode', 'errors')}">
                            <input type="text" maxLength="87" size="30" onchange="getAccount(this, '${createLink(controller: 'document', action: 'account')}', 'invoice')" id="lines[${i}].accountCode" name="lines[${i}].accountCode" value="${display(bean: documentLine, field: 'accountCode')}"/>
                        </td>
                        <td class="narrow"><input type="text" maxLength="50" size="40" id="lines[${i}].description" name="lines[${i}].description" value="${display(bean: documentLine, field: 'description')}"/></td>
                        <td class="narrow"><g:select optionKey="id" optionValue="code" from="${taxCodeList}" name="${'lines[' + i + '].taxCode.id'}" value="${documentLine?.taxCode?.id}" noSelection="['null': msg(code: 'generic.no.selection', default: '-- none --')]"/></td>
                        <td class="narrow"><input type="text" size="12" id="lines[${i}].documentValue" name="lines[${i}].documentValue" value="${display(bean: documentLine, field: 'documentValue', scale: settings.decimals)}"/></td>
                        <td class="narrow"><input type="text" size="12" id="lines[${i}].documentTax" name="lines[${i}].documentTax" value="${display(bean: documentLine, field: 'documentTax', scale: settings.decimals)}"/></td>
                        <td class="narrow"><input type="text" size="12" id="lines[${i}].documentTotal" name="lines[${i}].documentTotal" value="${display(bean: documentLine, field: 'documentTotal', scale: settings.decimals)}"/></td>
                    </tr>
                    <tr>
                        <td class="narrow" colspan="2" style="padding-bottom:12px;"><input disabled="true" type="text" size="76" id="lines[${i}].displayName" name="lines[${i}].displayName" value="${display(bean: documentLine, field: 'accountName')}"/></td>
                        <td class="narrow"><input type="hidden" id="lines[${i}].accountName" name="lines[${i}].accountName" value="${display(bean: documentLine, field: 'accountName')}"/></td>
                        <td class="narrow"></td>
                        <td class="narrow"></td>
                        <td class="narrow"></td>
                    </tr>
                </g:each>
                </tbody>
            </table>
        </div>
        <div class="buttons">
            <span class="button"><g:actionSubmit class="add" action="auto" value="${msg(code:'document.auto.allocate', 'default':'Auto Allocation')}"/></span>
            <span class="button"><g:actionSubmit class="delete" action="invoicing" value="${msg(code:'document.no.allocate', 'default':'No Allocation')}"/></span>
            <span class="button"><g:actionSubmit class="save" action="manual" value="${msg(code:'document.manual.allocate', 'default':'Manual Allocation')}"/></span>
            <span class="button"><g:actionSubmit class="edit" action="lines" value="${msg(code:'document.more.lines', 'default':'More Lines')}"/></span>
        </div>
    </g:form>
</div>
</body>
</html>
