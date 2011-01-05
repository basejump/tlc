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
<input type="hidden" id="responseMessage" name="responseMessage" value="${msg(code: 'generic.ajax.response', default: 'Unable to understand the response from the server')}"/>
<input type="hidden" id="timeoutMessage" name="timeoutMessage" value="${msg(code: 'generic.ajax.timeout', default: 'Operation timed out waiting for a response from the server')}"/>
<div class="dialog">
    <table>
        <tbody>

        <tr class="prop">
            <td valign="top" class="name">
                <label for="sourceCode"><g:msg code="recurring.account" default="Bank Account"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: documentInstance, field: 'sourceCode', 'errors')}">
                <g:select disabled="${settings.disableInitial ? 'true' : ''}" initialField="${settings.enableInitial ? 'true' : ''}" onchange="accountChanged()" onblur="getBank(this, '${createLink(controller: 'document', action: 'bank')}', 'bank')" optionKey="code" optionValue="name" from="${bankAccountList}" name="sourceCode" value="${recurringInstance?.sourceCode}" noSelection="['': msg(code: 'generic.select', default: '-- select --')]"/>&nbsp;<g:help code="recurring.account"/>
            </td>

            <td valign="top" class="name">
                <label for="currency.id"><g:msg code="recurring.currency" default="Currency"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'currency', 'errors')}">
                <g:domainSelect disabled="${settings.disableInitial ? 'true' : ''}" name="currency.id" options="${currencyList}" selected="${recurringInstance?.currency}" prefix="currency.name" code="code" default="name"/>&nbsp;<g:help code="recurring.currency"/>
            </td>

            <td valign="top" class="name">
                <label for="type.id"><g:msg code="recurring.type" default="Document Type"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'type', 'errors')}">
                <g:domainSelect disabled="${settings.disableInitial ? 'true' : ''}" name="type.id" options="${documentTypeList}" selected="${recurringInstance?.type}" displays="${['code', 'name']}" noSelection="['': msg(code: 'generic.select', default: '-- select --')]"/>&nbsp;<g:help code="recurring.type"/>
            </td>
        </tr>

        <tr class="prop">
            <td valign="top" class="name">
                <label for="reference"><g:msg code="recurring.reference" default="Reference"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'reference', 'errors')}">
                <input ${settings.isComplete ? 'disabled="true"' : ''} initialField="${(settings.isOngoing && settings.disableInitial) ? 'true' : ''}" type="text" maxLength="24" size="24" id="reference" name="reference" value="${display(bean: recurringInstance, field: 'reference')}"/>&nbsp;<g:help code="recurring.reference"/>
            </td>

            <td valign="top" class="name">
                <label for="description"><g:msg code="recurring.description" default="Description"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'description', 'errors')}">
                <input ${settings.isComplete ? 'disabled="true"' : ''} type="text" maxlength="50" size="45" id="description" name="description" value="${display(bean: recurringInstance, field: 'description')}"/>&nbsp;<g:help code="recurring.description"/>
            </td>

            <td valign="top" class="name">
                <label for="autoAllocate"><g:msg code="recurring.autoAllocate" default="Auto Allocate"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'autoAllocate', 'errors')}">
                <g:checkBox disabled="${settings.isComplete ? 'true' : ''}" name="autoAllocate" value="${recurringInstance?.autoAllocate}"></g:checkBox>&nbsp;<g:help code="recurring.autoAllocate"/>
            </td>
        </tr>

        <tr class="prop">
            <td valign="top" class="name">
                <label for="totalTransactions"><g:msg code="recurring.totalTransactions" default="Total Transactions"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'totalTransactions', 'errors')}">
                <input ${settings.disableInitial ? 'disabled="true"' : ''} type="text" size="10" id="totalTransactions" name="totalTransactions" value="${display(bean: recurringInstance, field: 'totalTransactions')}"/>&nbsp;<g:help code="recurring.totalTransactions"/>
            </td>

            <td valign="top" class="name">
                <label for="processedCount"><g:msg code="recurring.processedCount" default="Processed Count"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'processedCount', 'errors')}">
                <input disabled="true" type="text" size="10" id="processedCount" name="processedCount" value="${display(bean: recurringInstance, field: 'processedCount')}"/>&nbsp;<g:help code="recurring.processedCount"/>
            </td>

            <td valign="top" class="name">
                <label for="nextDue"><g:msg code="recurring.nextDue" default="Next Due"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'processedCount', 'errors')}">
                <input disabled="true" type="text" size="20" id="nextDue" name="nextDue" value="${display(bean: recurringInstance, field: 'nextDue', scale: 1)}"/>&nbsp;<g:help code="recurring.nextDue"/>
            </td>
        </tr>

        <tr class="prop">
            <td valign="top" class="name">
                <label for="initialDate"><g:msg code="recurring.initialDate" default="Initial Date"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'initialDate', 'errors')}">
                <input ${settings.disableInitial ? 'disabled="true"' : ''} type="text" size="20" id="initialDate" name="initialDate" value="${display(bean: recurringInstance, field: 'initialDate', scale: 1)}"/>&nbsp;<g:help code="recurring.initialDate"/>
            </td>

            <td valign="top" class="name">
                <label for="recursFrom"><g:msg code="recurring.recursFrom" default="Recurs From"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'recursFrom', 'errors')}">
                <input ${settings.disableRecurring ? 'disabled="true"' : ''} type="text" size="20" id="recursFrom" name="recursFrom" value="${display(bean: recurringInstance, field: 'recursFrom', scale: 1)}"/>&nbsp;<g:help code="recurring.recursFrom"/>
            </td>

            <td></td>
            <td></td>
        </tr>

        <tr class="prop">
            <td valign="top" class="name">
                <label for="recurrenceType"><g:msg code="recurring.recurrenceType" default="Recurrence Type"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'recurrenceType', 'errors')}">
                <g:select disabled="${settings.disableRecurring ? 'true' : ''}" id="recurrenceType" name="recurrenceType" from="${recurringInstance.constraints.recurrenceType.inList}" value="${recurringInstance.recurrenceType}" valueMessagePrefix="recurring.recurrenceType"/>&nbsp;<g:help code="recurring.recurrenceType"/>
            </td>

            <td valign="top" class="name">
                <label for="recurrenceInterval"><g:msg code="recurring.recurrenceInterval" default="Recurrence Interval"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'recurrenceInterval', 'errors')}">
                <input ${settings.disableRecurring ? 'disabled="true"' : ''} type="text" size="10" id="recurrenceInterval" name="recurrenceInterval" value="${display(bean: recurringInstance, field: 'recurrenceInterval')}"/>&nbsp;<g:help code="recurring.recurrenceInterval"/>
            </td>

            <td valign="top" class="name">
                <label for="lastDayOfMonth"><g:msg code="recurring.lastDayOfMonth" default="Last Day Of Month"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'lastDayOfMonth', 'errors')}">
                <g:checkBox disabled="${settings.disableRecurring ? 'true' : ''}" name="lastDayOfMonth" value="${recurringInstance?.lastDayOfMonth}"/>&nbsp;<g:help code="recurring.lastDayOfMonth"/>
            </td>
        </tr>

        <tr class="prop">
            <td valign="top" class="name">
                <label for="initialValue"><g:msg code="recurring.initialValue" default="Initial Value"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'initialValue', 'errors')}">
                <input ${settings.disableInitial ? 'disabled="true"' : ''} type="text" size="15" id="initialValue" name="initialValue" value="${display(bean: recurringInstance, field: 'initialValue', scale: settings.decimals)}"/>&nbsp;<g:help code="recurring.initialValue"/>
            </td>

            <td valign="top" class="name">
                <label for="recurringValue"><g:msg code="recurring.recurringValue" default="Recurring Value"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'recurringValue', 'errors')}">
                <input ${settings.recurringComplete ? 'disabled="true"' : ''} type="text" size="15" id="recurringValue" name="recurringValue" value="${display(bean: recurringInstance, field: 'recurringValue', scale: settings.decimals)}"/>&nbsp;<g:help code="recurring.recurringValue"/>
            </td>

            <td valign="top" class="name">
                <label for="finalValue"><g:msg code="recurring.finalValue" default="Final Value"/>:</label>
            </td>
            <td valign="top" class="small-value ${hasErrors(bean: recurringInstance, field: 'finalValue', 'errors')}">
                <input ${settings.isComplete ? 'disabled="true"' : ''} type="text" size="15" id="finalValue" name="finalValue" value="${display(bean: recurringInstance, field: 'finalValue', scale: settings.decimals)}"/>&nbsp;<g:help code="recurring.finalValue"/>
            </td>
        </tr>
        </tbody>
    </table>
</div>
<div class="list entry">
    <table>
        <thead>
        <tr>
            <th><g:msg code="recurringLine.accountType" default="Ledger"/>&nbsp;<g:help code="recurringLine.accountType"/></th>
            <th><g:msg code="recurringLine.account" default="Account"/>&nbsp;<g:help code="recurringLine.account"/></th>
            <th><g:msg code="recurringLine.description" default="Description"/>&nbsp;<g:help code="recurringLine.description"/></th>
            <th><g:msg code="recurringLine.initialValue" default="Initial"/>&nbsp;<g:help code="recurringLine.initialValue"/></th>
            <th><g:msg code="recurringLine.recurringValue" default="Initial"/>&nbsp;<g:help code="recurringLine.recurringValue"/></th>
            <th><g:msg code="recurringLine.finalValue" default="Initial"/>&nbsp;<g:help code="recurringLine.finalValue"/></th>
        </tr>
        </thead>
        <tbody>
        <g:each in="${recurringInstance.lines}" status="i" var="recurringLine">
            <tr>
                <td class="narrow">
                    <g:select disabled="${recurringLine.used ? 'true' : ''}" id="lines[${i}].accountType" name="lines[${i}].accountType" from="${['gl','ar','ap']}" value="${recurringLine.accountType}" valueMessagePrefix="document.line.accountType" onchange="setAccount(this, '${createLink(controller: 'document', action: 'accounts')}', 'cash')"/>
                </td>
                <td class="narrow ${hasErrors(bean: recurringLine, field: 'accountCode', 'errors')}">
                    <input ${recurringLine.used ? 'disabled="true"' : ''} type="text" maxLength="87" size="30" onchange="setAccount(this, '${createLink(controller: 'document', action: 'accounts')}', 'cash')" id="lines[${i}].accountCode" name="lines[${i}].accountCode" value="${display(bean: recurringLine, field: 'accountCode')}"/>
                </td>
                <td class="narrow"><input ${settings.isComplete ? 'disabled="true"' : ''} type="text" maxLength="50" size="40" id="lines[${i}].description" name="lines[${i}].description" value="${display(bean: recurringLine, field: 'description')}"/></td>
                <td class="narrow"><input ${settings.disableInitial ? 'disabled="true"' : ''} type="text" size="12" id="lines[${i}].initialValue" name="lines[${i}].initialValue" value="${display(bean: recurringLine, field: 'initialValue', scale: settings.decimals)}"/></td>
                <td class="narrow"><input ${settings.recurringComplete ? 'disabled="true"' : ''} type="text" size="12" id="lines[${i}].recurringValue" name="lines[${i}].recurringValue" value="${display(bean: recurringLine, field: 'recurringValue', scale: settings.decimals)}"/></td>
                <td class="narrow"><input ${settings.isComplete ? 'disabled="true"' : ''} type="text" size="12" id="lines[${i}].finalValue" name="lines[${i}].finalValue" value="${display(bean: recurringLine, field: 'finalValue', scale: settings.decimals)}"/></td>
            </tr>
            <tr>
                <td class="narrow" colspan="3" style="padding-bottom:12px;"><input disabled="true" type="text" size="76" id="lines[${i}].displayName" name="lines[${i}].displayName" value="${display(bean: recurringLine, field: 'accountName')}"/></td>
                <td class="narrow">
                    <input type="hidden" id="lines[${i}].accountName" name="lines[${i}].accountName" value="${display(bean: recurringLine, field: 'accountName')}"/>
                    <input type="hidden" id="lines[${i}].used" name="lines[${i}].used" value="${recurringLine.used}"/>
                </td>
                <td></td>
                <td></td>
            </tr>
        </g:each>
        </tbody>
    </table>
</div>
