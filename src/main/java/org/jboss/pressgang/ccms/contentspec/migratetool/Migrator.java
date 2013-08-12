package org.jboss.pressgang.ccms.contentspec.migratetool;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecParser;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.provider.exception.ProviderException;
import org.jboss.pressgang.ccms.rest.v1.entities.contentspec.RESTTextCSProcessingOptionsV1;
import org.jboss.pressgang.ccms.rest.v1.entities.contentspec.RESTTextContentSpecV1;
import org.jboss.pressgang.ccms.rest.v1.jaxrsinterfaces.RESTInterfaceV1;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Migrator {
    private static final Logger log = LoggerFactory.getLogger(Migrator.class);

    private final RESTInterfaceV1 restInterface;
    private final RESTProviderFactory providerFactory;
    private final RESTTextCSProcessingOptionsV1 processingOptions = new RESTTextCSProcessingOptionsV1();

    public Migrator(final RESTProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
        restInterface = providerFactory.getRESTManager().getRESTClient();
        processingOptions.setPermissive(true);
    }

    public Map<Integer, LinkedHashMap<TopicWrapper, ContentSpec>> parseContentSpecs(final CollectionWrapper<TopicWrapper> contentSpecs) {
        final ContentSpecParser parser = new ContentSpecParser(providerFactory, new ErrorLoggerManager());
        final Map<Integer, LinkedHashMap<TopicWrapper, ContentSpec>> contentSpecMap = new HashMap<Integer, LinkedHashMap<TopicWrapper,
                ContentSpec>>();

        // Parse each content spec
        if (contentSpecs != null && !contentSpecs.isEmpty()) {
            for (final TopicWrapper contentSpec : contentSpecs.getItems()) {
                if (parser.parse(contentSpec.getXml(), ContentSpecParser.ParsingMode.EITHER, true)) {
                    if (parser.getContentSpec().getId() != null) {
                        if (!contentSpecMap.containsKey(contentSpec.getId())) {
                            contentSpecMap.put(contentSpec.getId(), new LinkedHashMap<TopicWrapper, ContentSpec>());
                        }
                        contentSpecMap.get(contentSpec.getId()).put(contentSpec, parser.getContentSpec());
                    } else {
                        log.error("Content Spec " + contentSpec.getId() + "-" + contentSpec.getRevision() + " is invalid");
                    }
                } else {
                    log.error("Content Spec " + contentSpec.getId() + "-" + contentSpec.getRevision() + " is invalid");
                }
            }
        }

        return contentSpecMap;
    }

    public Map<TopicWrapper, RESTTextContentSpecV1> migrateContentSpecs(
            final Map<Integer, LinkedHashMap<TopicWrapper, ContentSpec>> contentSpecMap) {
        log.info("Starting to migrate the content specifications...");

        // Sort each content spec so it's in order from the earliest to latest revision
        for (final Map.Entry<Integer, LinkedHashMap<TopicWrapper, ContentSpec>> entry : contentSpecMap.entrySet()) {
            ContentSpecSorter.sort(entry.getValue());
        }

        final Map<TopicWrapper, RESTTextContentSpecV1> retValue = new HashMap<TopicWrapper, RESTTextContentSpecV1>();
        final int showPercent = 5;
        final float total = contentSpecMap.size();
        float current = 0;
        int lastPercent = 0;

        // Perform the migration of each content spec
        for (final Map.Entry<Integer, LinkedHashMap<TopicWrapper, ContentSpec>> contentSpecEntry : contentSpecMap.entrySet()) {
            for (final Map.Entry<TopicWrapper, ContentSpec> contentSpecEntry2 : contentSpecEntry.getValue().entrySet()) {
                retValue.put(contentSpecEntry2.getKey(), migrateContentSpec(contentSpecEntry2.getKey(), contentSpecEntry2.getValue()));
                // Sleep just to give the server a rest
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {

                }
            }

            ++current;
            final int percent = Math.round(current / total * 100);
            if (percent - lastPercent >= showPercent) {
                lastPercent = percent;
                log.info("\tMigration " + percent + "% Done");
            }
        }

        log.info("Finished migrating the content specifications...");
        return retValue;
    }

    protected RESTTextContentSpecV1 migrateContentSpec(final TopicWrapper contentSpecEntity, final ContentSpec contentSpec) {
        try {
            final String textContentSpec = restInterface.getTEXTContentSpec(contentSpecEntity.getId());
            final String checksum = ContentSpecUtilities.getContentSpecChecksum(textContentSpec);
            String newTextContentSpec = contentSpec.toString();
            newTextContentSpec = ContentSpecUtilities.replaceChecksum(newTextContentSpec, checksum);

            final RESTTextContentSpecV1 newContentSpecEntity = new RESTTextContentSpecV1();
            newContentSpecEntity.setId(contentSpecEntity.getId());
            newContentSpecEntity.explicitSetText(newTextContentSpec);
            newContentSpecEntity.setProcessingOptions(processingOptions);

            return restInterface.updateJSONTextContentSpec("", newContentSpecEntity,
                    "Content Spec Migration from topic " + contentSpecEntity.getId() + "-" + contentSpecEntity.getRevision(), 1, "89");
        } catch (ClientResponseFailure responseFailure) {
            log.error("Failed to Migrate Content Spec " + contentSpecEntity.getId() + "-" + contentSpecEntity.getRevision(), responseFailure);
        } catch (ProviderException e) {
            log.error("Failed to Migrate Content Spec " + contentSpecEntity.getId() + "-" + contentSpecEntity.getRevision(), e);
        }

        return null;
    }
}
