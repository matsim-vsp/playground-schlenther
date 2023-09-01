/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.run.replaceCarByDRT;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
class PRStation {

	private final String name;
	private final Id<Link> linkId;
	private final Coord coord;

	PRStation(String name, Id<Link> linkId, Coord coord) {
		this.name = name;
		this.linkId = linkId;
		this.coord = coord;
	}

	protected Coord getCoord() {
		return coord;
	}

	protected String getName() {
		return name;
	}

	protected Id<Link> getLinkId() {
		return linkId;
	}
}
