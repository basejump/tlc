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
<%@ page import="com.whollygrails.tlc.books.Customer" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title><g:msg code="customer.transactions.for" args="${[customerInstance.name]}" default="Transaction List for Customer ${customerInstance.name}"/></title>
    <g:yuiResources require="connection"/>
</head>
<body>
<div class="nav">
    <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:msg code="home" default="Home"/></a></span>
    <span class="menuButton"><g:link class="menu" controller="systemMenu" action="display"><g:msg code="systemMenu.display" default="Menu"/></g:link></span>
</div>
<div class="body">
    <g:pageTitle code="customer.transactions.for" args="${[customerInstance.name]}" default="Transaction List for Customer ${customerInstance.name}" help="maintenance.transactions" returns="true"/>
    <g:if test="${flash.message}">
        <div class="message"><g:msg code="${flash.message}" args="${flash.args}" default="${flash.defaultMessage}"/></div>
    </g:if>
    <g:hasErrors bean="${customerInstance}">
        <div class="errors">
            <g:listErrors bean="${customerInstance}"/>
        </div>
    </g:hasErrors>
    <div id="ajaxErrorMessage" class="errors" style="visibility:hidden;"></div>
    <g:form method="post">
        <input type="hidden" id="responseMessage" name="responseMessage" value="${msg(code: 'generic.ajax.response', default: 'Unable to understand the response from the server')}"/>
        <input type="hidden" id="timeoutMessage" name="timeoutMessage" value="${msg(code: 'generic.ajax.timeout', default: 'Operation timed out waiting for a response from the server')}"/>
        <div class="dialog">
            <table>
                <tbody>
                <tr class="prop">
                    <td valign="top" class="name"><g:msg code="customer.code" default="Code"/>:</td>
                    <td valign="top" class="value">${display(bean: customerInstance, field: 'code')}</td>

                    <td valign="top" class="name"><g:msg code="customer.name" default="Name"/>:</td>
                    <td valign="top" class="value nowrap">${display(bean: customerInstance, field: 'name')}</td>

                    <td valign="top" class="name">
                        <label for="displayPeriod"><g:msg code="customer.enquire.period" default="Period"/>:</label>
                    </td>
                    <td valign="top" class="value nowrap">
                        <g:select optionKey="id" optionValue="code" from="${periodList}" name="displayPeriod" value="${displayPeriod?.id}" noSelection="['null': msg(code: 'generic.no.selection', default: '-- none --')]"/>&nbsp;<g:help code="customer.enquire.period"/>
                    </td>

                    <td><span class="button"><g:actionSubmit class="save" action="transactions" value="${msg(code:'generic.enquire', 'default':'Enquire')}"/></span></td>
                </tr>

                <tr>
                    <td valign="top" class="name nowrap"><g:msg code="customer.accountCurrentBalance" default="Current Balance"/>:</td>
                    <td valign="top" class="value"><g:cr context="${customerInstance}" field="balance"/></td>

                    <td valign="top" class="name"><g:msg code="customer.currency" default="Currency"/>:</td>
                    <td valign="top" class="value nowrap">${msg(code: 'currency.name.' + customerInstance.currency.code, default: customerInstance.currency.name)}</td>

                    <td></td>
                    <td></td>
                    <td></td>
                </tr>
                </tbody>
            </table>
        </div>
        <div class="buttons">
            <span class="button"><g:actionSubmit class="add" action="auto" value="${msg(code:'document.auto.allocate', 'default':'Auto Allocation')}"/></span>
        </div>
    </g:form>
    <div class="list">
        <table>
            <thead>
            <tr>

                <th><g:msg code="generic.document" default="Document"/></th>

                <th><g:msg code="generic.documentDate" default="Document Date"/></th>

                <th><g:msg code="generalTransaction.description" default="Description"/></th>

                <th><g:msg code="document.dueDate" default="Due Date"/></th>

                <th><g:msg code="generalTransaction.onHold" default="On Hold"/></th>

                <th class="right"><g:msg code="generalTransaction.accountUnallocated" default="Unallocated"/></th>

                <th class="right"><g:msg code="generic.debit" default="Debit"/></th>

                <th class="right"><g:msg code="generic.credit" default="Credit"/></th>

                <th><g:msg code="sales.manual" default="Manual Allocation"/></th>

            </tr>
            </thead>
            <form method="post">
                <tbody>
                <g:each in="${transactionInstanceList}" status="i" var="transactionInstance">
                    <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">

                        <td><g:format value="${transactionInstance.document.type.code + transactionInstance.document.code}"/></td>

                        <td><g:format value="${transactionInstance.document.documentDate}" scale="1"/></td>

                        <td>${display(bean: transactionInstance, field: 'description')}</td>

                        <td><g:format value="${transactionInstance.document.dueDate}" scale="1"/></td>

                        <td valign="top" class="value center">
                            <g:checkBox name="onHold[${transactionInstance.id}]" value="${transactionInstance.onHold}" onclick="setHold(this, '${createLink(controller: 'document', action: 'hold')}')"></g:checkBox>
                        </td>

                        <td class="right"><g:drcr context="${customerInstance}" line="${transactionInstance}" field="unallocated" zeroIsNull="true"/></td>

                        <td class="right"><g:debit context="${customerInstance}" line="${transactionInstance}" field="value"/></td>

                        <td class="right"><g:credit context="${customerInstance}" line="${transactionInstance}" field="value"/></td>

                        <td>
                            <g:if test="${transactionInstance.accountValue}">
                                <g:drilldown controller="customer" action="allocate" domain="GeneralTransaction" value="${transactionInstance.id}" params="${[transactionPeriod: displayPeriod?.id]}"/>
                            </g:if>
                        </td>

                    </tr>
                </g:each>
                </tbody>
            </form>
        </table>
    </div>
    <div class="paginateButtons">
        <g:paginate total="${transactionInstanceTotal}" params="${[displayPeriod: displayPeriod?.id]}"/>
    </div>
</div>
</body>
</html>
