/*******************************************************************************
 Copyright (c) 2013 James Richardson.

 BukkitRestoreTask.java is part of TimedRestore.

 TimedRestore is free software: you can redistribute it and/or modify it
 under the terms of the GNU General Public License as published by the Free
 Software Foundation, either version 3 of the License, or (at your option) any
 later version.

 TimedRestore is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 TimedRestore. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package name.richardson.james.bukkit.timedrestore.scheduler;

import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.snapshots.InvalidSnapshotException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import name.richardson.james.bukkit.utilities.logging.PrefixedLogger;

import name.richardson.james.bukkit.timedrestore.TimedRestorePlugin;
import name.richardson.james.bukkit.timedrestore.persistence.TaskConfigurationEntry;
import name.richardson.james.bukkit.timedrestore.region.InvalidRegionException;
import name.richardson.james.bukkit.timedrestore.region.InvalidWorldException;
import name.richardson.james.bukkit.timedrestore.region.RestoreRegion;

// TODO: Auto-generated Javadoc

/**
 * This {@link RestoreTask} implementation represents the class that actually commits the restoration of a region. It is
 * responsible for creating the associated {@link RestoreRegion} to restore, committing that restore operation and
 * handling any exceptions that may happen as a consequence.
 */
public class BukkitRestoreTask extends AbstractRestoreTask {

	final private static BukkitScheduler scheduler = Bukkit.getScheduler();

	private final Plugin plugin;

	private CronRestoreTask cronTask;
	private int id;

	/**
	 * Instantiates a new BukkitRestoreTask. This task registers itself with the Bukkit region as soon as it is created.
	 *
	 * @param configuration the backing configuration for this task
	 * @param plugin        the plugin
	 */
	public BukkitRestoreTask(final TaskConfigurationEntry configuration, final Plugin plugin, final CronRestoreTask cronTask) {
		super(configuration);
		this.plugin = plugin;
		this.cronTask = cronTask;
		this.schedule();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see name.richardson.james.bukkit.timedrestore.region.Task#deschedule()
	 */
	public void deschedule() {
		BukkitRestoreTask.scheduler.cancelTask(this.id);
		logger.log(Level.FINE, "Descheduled BukkitTask");
	}

	/**
	 * Run this restore operation using the {@link TaskConfigurationEntry} as the parameters.
	 *
	 * This creates a {@link RestoreRegion} with the parameters supplied and then attempts to commit the selected snapshot
	 * to the world. If any checked exceptions are thrown, this method will handle them and log appropriate warnings to the
	 * server log. Additionally this method will also deschedule any associated {@link CronRestoreTask} on an exception to
	 * prevent another {@link BukkitRestoreTask} with the same configuration being scheduled again.
	 */
	public void run() {
		for (final String regionName : this.getConfiguration().getRegions()) {
			logger.log(Level.INFO, "Restoring region: " + regionName);
			try {
				final RestoreRegion region = new RestoreRegion(this.getConfiguration().getWorldName(), regionName);
				region.restore(this.getConfiguration().getSnapshotDate());
			} catch (final InvalidSnapshotException e) {
				logger.log(Level.WARNING, "Snapshot `{0}` does not exist!", this.getConfiguration().getSnapshotDate());
				this.cronTask.deschedule();
			} catch (final IOException e) {
				logger.log(Level.WARNING, "Unable to restore region.");
				e.printStackTrace();
				this.cronTask.deschedule();
			} catch (final DataException e) {
				logger.log(Level.WARNING, "Unable to restore region.");
				e.printStackTrace();
				this.cronTask.deschedule();
			} catch (final InvalidWorldException e) {
				logger.log(Level.WARNING, "World `{0}` does not exist!", e.getWorldName());
				this.cronTask.deschedule();
			} catch (final InvalidRegionException e) {
				final Object[] params = {e.getWorldName(), e.getRegionName()};
				logger.log(Level.WARNING, "Region `{0}:{1}` does not exist!", params);
				this.cronTask.deschedule();
			}
		}
	}

	/**
	 * Schedule this task with the {@link BukkitScheduler} if {@link TimedRestorePlugin} is enabled.
	 *
	 * The task will be scheduled as a synchronous delayed task that should be completed as soon as possible.
	 */
	public void schedule() {
		if (this.plugin.isEnabled()) {
			this.id = BukkitRestoreTask.scheduler.scheduleSyncDelayedTask(this.plugin, this);
			logger.log(Level.FINE, "Scheduled BukkitTask");
		}
	}

}
