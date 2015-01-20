/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.svclayer.outage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.extremecomponents.table.context.Context;
import org.extremecomponents.table.context.HttpServletRequestContext;
import org.extremecomponents.table.limit.Limit;
import org.extremecomponents.table.limit.LimitFactory;
import org.extremecomponents.table.limit.TableLimit;
import org.extremecomponents.table.limit.TableLimitFactory;
import org.opennms.netmgt.model.OnmsOutage;

/**
 * <p>OutageTable class.</p>
 *
 * @author <a href="mailto:joed@opennms.org">Johan Edstrom</a>
 * @version $Id: $
 * @since 1.8.1
 */
public class OutageTable {

    OutageListBuilder m_cview = new OutageListBuilder();

    Collection<OnmsOutage> foundOutages;

    Collection<OnmsOutage> viewOutages;

    private static final int ROW_LIMIT = 25;

    /**
     * <p>getResolvedOutageTable</p>
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @param reply a {@link javax.servlet.http.HttpServletResponse} object.
     * @param m_outageService a {@link org.opennms.web.svclayer.outage.OutageService} object.
     * @return a {@link java.util.Map} object.
     */
    public Map<String, ?> getResolvedOutageTable(HttpServletRequest request,
            HttpServletResponse reply, OutageService m_outageService) {

        Context context = new HttpServletRequestContext(request);
        LimitFactory limitFactory = new TableLimitFactory(context,
                                                          "tabledata");
        Limit limit = new TableLimit(limitFactory);

        OutagesFilteringView m_filterService = new OutagesFilteringView();

        String searchFilter = m_filterService.filterQuery(request);

        Map<String, Object> myModel = new HashMap<String, Object>();

        if (searchFilter.equals("")) {
            searchFilter = " AND 1=1 ";
        }

        Integer totalRows = m_outageService.outageResolvedCountFiltered(searchFilter);
        limit.setRowAttributes(totalRows, ROW_LIMIT);

        if (limit.getPage() == 1) {
            // no offset set
            myModel.put("rowStart", 0);
            context.setRequestAttribute("rowStart", 0);
            context.setRequestAttribute("rowEnd", ROW_LIMIT);
            myModel.put("rowEnd", ROW_LIMIT);

            if (limit.getSort().getProperty() == null) {
                foundOutages = m_outageService.getResolvedOutagesByRange(
                                                                         0,
                                                                         ROW_LIMIT,
                                                                         "iflostservice",
                                                                         "asc",
                                                                         searchFilter);

            } else {
                foundOutages = m_outageService.getResolvedOutagesByRange(
                                                                         0,
                                                                         ROW_LIMIT,
                                                                         "outages."
                                                                                 + limit.getSort().getProperty(),
                                                                         limit.getSort().getSortOrder(),
                                                                         searchFilter);

            }
            myModel.put("begin", 0);
            myModel.put("end", ROW_LIMIT);

        } else {

            Integer rowstart = null;
            Integer rowend = null;

            // quirky situation... - as we started on 0 (zero)
            rowstart = ((limit.getPage() * ROW_LIMIT + 1) - ROW_LIMIT);
            rowend = (ROW_LIMIT);
            myModel.put("begin", rowstart);
            myModel.put("end", rowend);

            if (limit.getSort().getProperty() == null) {
                foundOutages = m_outageService.getResolvedOutagesByRange(
                                                                         rowstart,
                                                                         rowend,
                                                                         "iflostservice",
                                                                         "asc",
                                                                         searchFilter);

            } else {

                foundOutages = m_outageService.getResolvedOutagesByRange(
                                                                         rowstart,
                                                                         rowend,
                                                                         "outages."
                                                                                 + limit.getSort().getProperty()
                                                                                 + ",outageid",
                                                                         limit.getSort().getSortOrder(),
                                                                         searchFilter);

            }
        }

        Collection<Map<String,Object>> theTable = m_cview.theTable(foundOutages);

        myModel.put("searchfilter", searchFilter);
        myModel.put("tabledata", theTable);
        myModel.put("totalRows", totalRows);
        myModel.put("suffix", request.getQueryString());

        return myModel;
    }

}