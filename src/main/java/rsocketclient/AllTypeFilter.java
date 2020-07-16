package rsocketclient;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Log4j2
class AllTypeFilter implements TypeFilter {

	private final List<TypeFilter> delegates;

	AllTypeFilter(TypeFilter... delegates) {
		this.delegates = Arrays.asList(delegates);
	}

	@Override
	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
		for (TypeFilter filter : this.delegates) {
			if (!filter.match(metadataReader, metadataReaderFactory)) {
				return false;
			}
		}
		log.info("returning true for " + metadataReader.toString());
		return true;
	}

}
