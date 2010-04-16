/*
 *  $Id: VirtModel.java,v 1.3 2010/02/12 10:26:07 source Exp $
 *
 *  This file is part of the OpenLink Software Virtuoso Open-Source (VOS)
 *  project.
 *
 *  Copyright (C) 1998-2008 OpenLink Software
 *
 *  This project is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation; only version 2 of the License, dated June 1991.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */
package virtuoso.jena.driver;


import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.impl.ModelCom;


public class VirtModel extends ModelCom {

    /**
     * @param base
     */
    public VirtModel(VirtGraph base) 
    {
        super(base);
    }
	
	
    public static VirtModel openDefaultModel(String url, String user, 
	String password) 
    {
    	return new VirtModel(new VirtGraph(url, user, password));
    }


    public static Model openDatabaseModel(String graphName, String url, 
    	String user, String password) 
    {
	return new VirtModel(new VirtGraph(graphName, url, user, password));
    }

//--java5 or newer    @Override
    public Model removeAll() 
    {
	try {
	        VirtGraph _graph=(VirtGraph)this.graph;
	        _graph.clear();
	} catch (ClassCastException e) {
		super.removeAll();
	}
	return this;
    }
	
}
