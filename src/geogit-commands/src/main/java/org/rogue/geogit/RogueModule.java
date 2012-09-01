package org.rogue.geogit;

import org.rogue.geogit.commands.Config;

import com.google.inject.AbstractModule;

public class RogueModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(Config.class);
	}

}
