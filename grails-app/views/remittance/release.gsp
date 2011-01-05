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
    <title><g:msg code="remittance.release" default="Release Auto-Payments"/></title>
</head>
<body>
<div class="nav">
    <span class="menuButton"><a class="home" href="${createLink(uri: '/')}"><g:msg code="home" default="Home"/></a></span>
    <span class="menuButton"><g:link class="menu" controller="systemMenu" action="display"><g:msg code="systemMenu.display" default="Menu"/></g:link></span>
</div>
<div class="body">
    <g:pageTitle code="remittance.release" default="Release Auto-Payments"/>
    <g:if test="${summaries}">
        <g:if test="${flash.message}">
            <div class="message"><g:msg code="${flash.message}" args="${flash.args}" default="${flash.defaultMessage}"/></div>
        </g:if>
        <g:hasErrors bean="${supplierInstance}">
            <div class="errors">
                <g:listErrors bean="${supplierInstance}"/>
            </div>
        </g:hasErrors>
        <div style="margin:10px;text-align:center;font-size:12px;font-weight:bold;">
            <g:msg code="generic.task.submit" default="Click the Submit button to run the report."/>
        </div>
        <h2><g:msg code="generic.summary" default="Summary"/></h2>
        <div class="list">
            <table>
                <thead>
                <tr>

                    <th><g:msg code="bank.account" default="Bank Account"/></th>

                    <th><g:msg code="account.name" default="Name"/></th>

                    <th><g:msg code="remittance.currency" default="Currency"/></th>

                    <th class="right"><g:msg code="remittance.total" default="Payment"/></th>

                </tr>
                </thead>
                <tbody>
                <g:each in="${summaries}" status="i" var="summary">
                    <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">

                        <td><g:display value="${summary.bankCode}"/></td>

                        <td><g:display value="${summary.bankName}"/></td>

                        <td><g:display value="${summary.currencyCode}"/></td>

                        <td class="right"><g:display value="${summary.accountValue}" scale="${summary.currencyDecimals}"/></td>

                    </tr>
                </g:each>
                </tbody>
            </table>
        </div>
        <p>&nbsp;</p>
        <p>&nbsp;</p>
        <g:form action="releasing" method="post">
            <div class="dialog">
                <table>
                    <tbody>

                    <tr class="prop">
                        <td valign="top" class="name">
                            <label for="taxId"><g:msg code="remittance.batchSize" default="Batch Size"/>:</label>
                        </td>
                        <td valign="top" class="value ${hasErrors(bean: supplierInstance, field: 'taxId', 'errors')}">
                            <input type="text" maxlength="5" size="5" id="taxId" name="taxId" value="${display(bean: supplierInstance, field: 'taxId')}"/>&nbsp;<g:help code="remittance.batchSize"/>
                        </td>
                    </tr>

                    <tr class="prop">
                        <td valign="top" class="name">
                            <label for="preferredStart"><g:msg code="queuedTask.demand.delay" default="Delay Until"/>:</label>
                        </td>
                        <td valign="top" class="value">
                            <input type="text" size="20" id="preferredStart" name="preferredStart" value=""/>&nbsp;<g:help code="queuedTask.demand.delay"/>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
            <div class="buttons">
                <span class="button"><input class="save" type="submit" value="${msg(code: 'generic.submit', 'default': 'Submit')}"/></span>
            </div>
        </g:form>
    </g:if>
    <g:else>
        <div style="margin:30px;text-align:center;font-size:12px;font-weight:bold;">
            <g:msg code="remittance.no.pending" default="There are no authorized remittance advices awaiting payment."/>
        </div>
    </g:else>
</div>
</body>
</html>
