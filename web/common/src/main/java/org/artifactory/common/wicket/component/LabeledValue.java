/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.common.wicket.component;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.common.wicket.behavior.CssClass;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class LabeledValue extends Panel {

    public LabeledValue(String id, String label) {
        this(id, label, null);
    }

    public LabeledValue(String id, String label, String value) {
        super(id, new Model(value));
        add(new CssClass("labeled-value"));

        add(new Label("label", label));
        add(new Label("value", getModel()));
    }

    public void setValue(String value) {
        getModel().setObject(value);
    }
}