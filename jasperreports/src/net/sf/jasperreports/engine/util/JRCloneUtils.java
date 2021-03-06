/*
 * JasperReports - Free Java Reporting Library.
 * Copyright (C) 2001 - 2009 Jaspersoft Corporation. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of JasperReports.
 *
 * JasperReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JasperReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with JasperReports. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jasperreports.engine.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.jasperreports.engine.JRCloneable;

/**
 * Cloning related utility methods.
 * 
 * @author Lucian Chirita (lucianc@users.sourceforge.net)
 * @version $Id$
 */
public final class JRCloneUtils
{

	/**
	 * Clones an object after checking whether the argument is null.
	 * 
	 * @param original the object to be cloned
	 * @return a clone of the argument, or <code>null</code> if the argument was
	 * <code>null</code>
	 */
	public static Object nullSafeClone(JRCloneable original)
	{
		return original == null ? null : original.clone();
	}
	
	/**
	 * Clones a list of objects.
	 * 
	 * The list elements are assumed to implement {@link JRCloneable}.
	 * 
	 * @param items the list to clone
	 * @return a new list which contains clones of the elements in the original list
	 */
	public static List cloneList(List items)
	{
		List clone;
		if (items == null)
		{
			clone = null;
		}
		else
		{
			clone = new ArrayList(items.size());
			for (Iterator it = items.iterator(); it.hasNext();)
			{
				JRCloneable item = (JRCloneable) it.next();
				clone.add(item.clone());
			}
		}
		return clone;
	}
	

	private JRCloneUtils()
	{
	}
}
