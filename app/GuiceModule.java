import services.StringService;
import services.StringServiceImpl;

import com.google.inject.AbstractModule;

public class GuiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(StringService.class).to(StringServiceImpl.class);
	}
}