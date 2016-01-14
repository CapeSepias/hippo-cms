/*
 * Copyright 2015-2015 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.diagnosis;

import javax.servlet.ServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.Application;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.hippoecm.frontend.Main;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.diagnosis.TaskLogFormatUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default {@link IRequestCycleListener} implementation to set diagnosis context and report monitoring logs.
 */
public class DiagnosisRequestCycleListener extends AbstractRequestCycleListener {

    private static Logger log = LoggerFactory.getLogger(DiagnosisRequestCycleListener.class);

    @Override
    public void onBeginRequest(RequestCycle cycle) {
        final DiagnosisService diagnosisService = HippoServiceRegistry.getService(DiagnosisService.class);

        if (diagnosisService != null) {
            final Main application = (Main) Application.get();
            final String remoteAddr = getFarthestRemoteAddr(cycle);

            if (diagnosisService.isEnabledFor(cycle.getRequest())) {
                if (HDC.isStarted()) {
                    log.error("HDC was not cleaned up properly in previous request cycle for some reason. So clean up HDC to start new one.");
                    HDC.cleanUp();
                }

                Task rootTask = HDC.start(application.getPluginApplicationName());
                rootTask.setAttribute("request", cycle.getRequest().getUrl().toString());
            }
        }
    }

    @Override
    public void onEndRequest(RequestCycle cycle) {
        if (HDC.isStarted()) {
            try {
                final Task rootTask = HDC.getRootTask();
                rootTask.stop();

                final DiagnosisService diagnosisService = HippoServiceRegistry.getService(DiagnosisService.class);
                final long threshold = diagnosisService != null ? diagnosisService.getThresholdMillisec() : -1;
                final int depth = diagnosisService != null ? diagnosisService.getDepth() : -1;

                if (threshold > -1L && rootTask.getDurationTimeMillis() < threshold) {
                    log.debug("Skipping task '{}' because took only '{}' ms.",
                              rootTask.getName(), rootTask.getDurationTimeMillis());
                } else {
                    log.info("Diagnosis Summary:\n{}", TaskLogFormatUtils.getTaskLog(rootTask, depth));
                }
            } finally {
                HDC.cleanUp();
            }
        }
    }

    protected String getFarthestRemoteAddr(final RequestCycle requestCycle) {
        String [] remoteAddrs = getRemoteAddrs(requestCycle);

        if (ArrayUtils.isNotEmpty(remoteAddrs)) {
            return remoteAddrs[0];
        }

        return null;
    }

    private String [] getRemoteAddrs(final RequestCycle requestCycle) {
        WebRequest request = (WebRequest) requestCycle.getRequest();

        String xff = request.getHeader("X-Forwarded-For");

        if (xff != null) {
            String [] addrs = xff.split(",");

            for (int i = 0; i < addrs.length; i++) {
                addrs[i] = addrs[i].trim();
            }

            return addrs;
        } else if (request.getContainerRequest() instanceof ServletRequest) {
            ServletRequest servletRequest = (ServletRequest) request.getContainerRequest();
            return new String [] { servletRequest.getRemoteAddr() };
        }

        return ArrayUtils.EMPTY_STRING_ARRAY;
    }

}
