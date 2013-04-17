/*
 * Copyright (C) 2004-2013 L2J DataPack
 * 
 * This file is part of L2J DataPack.
 * 
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.targethandlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.l2jserver.Config;
import com.l2jserver.gameserver.GeoData;
import com.l2jserver.gameserver.handler.ITargetTypeHandler;
import com.l2jserver.gameserver.model.L2Object;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.instance.L2SiegeFlagInstance;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.model.skills.targets.L2TargetType;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.util.Rnd;

/**
 * @author Adry_85
 */
public class AreaFriendly implements ITargetTypeHandler
{
	@Override
	public L2Object[] getTargetList(L2Skill skill, L2Character activeChar, boolean onlyFirst, L2Character target)
	{
		List<L2Character> targetList = new ArrayList<>();
		if (!checkTarget(activeChar, target) && (skill.getCastRange() >= 0))
		{
			activeChar.sendPacket(SystemMessageId.TARGET_IS_INCORRECT);
			return _emptyTargetList;
		}
		
		if (skill.getCastRange() >= 0)
		{
			if (onlyFirst)
			{
				return new L2Character[]
				{
					target
				};
			}
			
			if (activeChar.getActingPlayer().isInOlympiadMode())
			{
				return new L2Character[]
				{
					activeChar
				};
			}
			targetList.add(target); // Add target to target list
		}
		
		if (target != null)
		{
			int[] affectLimit = skill.getAffectLimit();
			// calculate maximum affect limit between min and max values
			int randomMax = Rnd.get(affectLimit[0], affectLimit[1]);
			int curTargets = 0;
			final Collection<L2Character> objs = target.getKnownList().getKnownCharactersInRadius(skill.getAffectRange());
			
			// TODO: Chain Heal - The recovery amount decreases starting from the most injured person.
			Collections.sort(targetList, new CharComparator());
			
			for (L2Character obj : objs)
			{
				if (!checkTarget(activeChar, obj) || (obj == activeChar))
				{
					continue;
				}
				
				targetList.add(obj);
				
				curTargets++;
				if (curTargets >= randomMax)
				{
					break;
				}
			}
		}
		
		if (targetList.isEmpty())
		{
			return _emptyTargetList;
		}
		return targetList.toArray(new L2Character[targetList.size()]);
	}
	
	private boolean checkTarget(L2Character activeChar, L2Character target)
	{
		if ((Config.GEODATA > 0) && !GeoData.getInstance().canSeeTarget(activeChar, target))
		{
			return false;
		}
		
		if ((target == null) || target.isDead() || target.isAlikeDead() || target.isDoor() || (target instanceof L2SiegeFlagInstance) || target.isMonster())
		{
			return false;
		}
		
		if ((target.getActingPlayer() != null) && (target.getActingPlayer().inObserverMode() || target.getActingPlayer().isInOlympiadMode()))
		{
			return false;
		}
		
		if ((activeChar.getActingPlayer().getClan() != null) && (target.getActingPlayer().getClan() != null))
		{
			if (activeChar.getActingPlayer().getClanId() != target.getActingPlayer().getClanId())
			{
				return false;
			}
		}
		
		if ((activeChar.getActingPlayer().getAllyId() != 0) && (target.getActingPlayer().getAllyId() != 0))
		{
			if (activeChar.getActingPlayer().getAllyId() != target.getActingPlayer().getAllyId())
			{
				return false;
			}
		}
		
		if ((target != activeChar) && (target.getActingPlayer() != null) && (target.getActingPlayer().getPvpFlag() > 0))
		{
			return false;
		}
		return true;
	}
	
	public class CharComparator implements Comparator<L2Character>
	{
		@Override
		public int compare(L2Character char1, L2Character char2)
		{
			return Integer.compare((int) (char1.getCurrentHp() / char1.getMaxHp()), (int) (char2.getCurrentHp() / char2.getMaxHp()));
		}
	}
	
	@Override
	public Enum<L2TargetType> getTargetType()
	{
		return L2TargetType.AREA_FRIENDLY;
	}
}