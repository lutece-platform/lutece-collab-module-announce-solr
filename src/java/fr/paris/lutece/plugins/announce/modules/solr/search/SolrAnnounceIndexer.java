/*
 * Copyright (c) 2002-2013, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.announce.modules.solr.search;

import fr.paris.lutece.plugins.announce.business.Announce;
import fr.paris.lutece.plugins.announce.business.AnnounceHome;
import fr.paris.lutece.plugins.announce.service.AnnouncePlugin;
import fr.paris.lutece.plugins.announce.service.announcesearch.AnnounceSearchItem;
import fr.paris.lutece.plugins.announce.utils.AnnounceUtils;
import fr.paris.lutece.plugins.search.solr.business.field.Field;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexer;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexerService;
import fr.paris.lutece.plugins.search.solr.indexer.SolrItem;
import fr.paris.lutece.plugins.search.solr.util.SolrConstants;
import fr.paris.lutece.portal.service.content.XPageAppService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.url.UrlItem;

import org.apache.lucene.demo.html.HTMLParser;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


/**
 * The Announce indexer for Solr search platform
 */
public class SolrAnnounceIndexer implements SolrIndexer
{

    private static final String PARAMETER_ANNOUNCE_ID = "announce_id";

    private static final String PROPERTY_DESCRIPTION = "announce-solr.indexer.description";
    private static final String PROPERTY_NAME = "announce-solr.indexer.name";
    private static final String PROPERTY_VERSION = "announce-solr.indexer.version";
    private static final String PROPERTY_INDEXER_ENABLE = "announce-solr.indexer.enable";
    private static final String PROPERTY_TAGS_LABEL = "announce-solr.indexer.tags.label";
    private static final String PROPERTY_TAGS_DESCRIPTION = "announce-solr.indexer.tags.description";
    private static final String SHORT_NAME = "announce";

    private static final String BLANK = " ";
    private static final List<String> LIST_RESSOURCES_NAME = new ArrayList<String>(  );

    private static final String ANNOUNCE_INDEXATION_ERROR = "[SolrAnnounceIndexer] An error occured during the indexation of the announce number ";
    
    /**
     * Default constructor
     */
    public SolrAnnounceIndexer(  )
    {
        super(  );

        LIST_RESSOURCES_NAME.add( Announce.RESOURCE_TYPE );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_DESCRIPTION );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_NAME );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_VERSION );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> indexDocuments(  )
    {
        Plugin plugin = PluginService.getPlugin( AnnouncePlugin.PLUGIN_NAME );

        List<String> lstErrors = new ArrayList<String>(  );
        
        // Announces
        // Add SolrItem for each announce 
        for ( Announce announce : AnnounceHome.findAllPublished( ) )
        {
        	try
        	{
        		if ( !announce.getSuspended(  ) && !announce.getSuspendedByUser(  ) )
        		{
        			UrlItem urlAnnounce = new UrlItem( SolrIndexerService.getBaseUrl(  ) );
        			urlAnnounce.addParameter( XPageAppService.PARAM_XPAGE_APP,
                            AppPropertiesService.getProperty( AnnounceUtils.PARAMETER_PAGE_ANNOUNCE ) );
                    urlAnnounce.addParameter( PARAMETER_ANNOUNCE_ID, announce.getId( ) );

        			SolrItem item = getDocument( announce, urlAnnounce.getUrl(  ), plugin );
        			SolrIndexerService.write( item );
        		}
        	}
        	catch ( Exception e )
        	{
        		lstErrors.add( SolrIndexerService.buildErrorMessage( e ) );
				AppLogService.error( ANNOUNCE_INDEXATION_ERROR + announce.getId(  ), e );
			}
        }
        
        return lstErrors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnable(  )
    {
        return "true".equalsIgnoreCase( AppPropertiesService.getProperty( PROPERTY_INDEXER_ENABLE ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Field> getAdditionalFields(  )
    {
        List<Field> fields = new ArrayList<Field>(  );
        Field field = new Field(  );
        field.setEnableFacet( false );
        field.setName( AnnounceSearchItem.FIELD_TAGS );
        field.setLabel( AppPropertiesService.getProperty( PROPERTY_TAGS_LABEL ) );
        field.setDescription( AppPropertiesService.getProperty( PROPERTY_TAGS_DESCRIPTION ) );
        fields.add( field );

        return fields;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SolrItem> getDocuments( String strDocument )
    {
        List<SolrItem> listDocs = new ArrayList<SolrItem>(  );
        Plugin plugin = PluginService.getPlugin( AnnouncePlugin.PLUGIN_NAME );

        for ( Announce announce : AnnounceHome.findAllPublished( ) )
        {
            if ( !announce.getSuspended(  ) && !announce.getSuspendedByUser(  ) )
            {
                UrlItem urlAnnounce = new UrlItem( SolrIndexerService.getBaseUrl(  ) );
                urlAnnounce.addParameter( XPageAppService.PARAM_XPAGE_APP, AnnounceUtils.PARAMETER_PAGE_ANNOUNCE ); //FIXME
                urlAnnounce.addParameter( PARAMETER_ANNOUNCE_ID, announce.getId( ) );

                SolrItem docAnnounce;

                try
                {
                    docAnnounce = getDocument( announce, urlAnnounce.getUrl(  ), plugin );
                    listDocs.add( docAnnounce );
                }
                catch ( Exception e )
                {
                    throw new RuntimeException(  );
                }
            }
        }

        return listDocs;
    }

    /**
     * Builds a document which will be used by Lucene during the indexing of the announces list
     * @param announce the announce
     * @param strUrl the url
     * @param plugin the plugin
     * @throws java.io.IOException I/O exceiption
     * @throws java.lang.InterruptedException interrupted exception
     * @return the document
     */
    private SolrItem getDocument( Announce announce, String strUrl, Plugin plugin )
        throws IOException, InterruptedException
    {
        // make a new, empty document
        SolrItem item = new SolrItem(  );

        // Setting the Categorie field
        List<String> categories = new ArrayList<String>(  );
        categories.add( announce.getCategory(  ).getLabel(  ) );
        item.setCategorie( categories );

        // Setting the tags list
        item.addDynamicField( AnnounceSearchItem.FIELD_TAGS, announce.getTags( ) );

        // Setting the URL field
        item.setUrl( strUrl );

        // Setting the Uid field
        item.setUid( getResourceUid( String.valueOf( announce.getId( ) ), Announce.RESOURCE_TYPE ) );

        // Setting the Date field
        item.setDate( announce.getDateCreation(  ) );

        // Setting the Content field
        String strContentToIndex = getContentToIndex( announce, plugin );

        StringReader readerPage = new StringReader( strContentToIndex );
        HTMLParser parser = new HTMLParser( readerPage );

        Reader reader = parser.getReader(  );
        int c;
        StringBuffer sb = new StringBuffer(  );

        while ( ( c = reader.read(  ) ) != -1 )
        {
            sb.append( String.valueOf( (char) c ) );
        }

        reader.close(  );
        item.setContent( sb.toString(  ) );

        // Setting the Title field
        item.setTitle( announce.getTitle(  ) );

        // Setting the Site field
        item.setSite( SolrIndexerService.getWebAppName(  ) );

        // Setting the Type field
        item.setType( AnnouncePlugin.PLUGIN_NAME );

        // return the item
        return item;
    }

    /**
     * Set the Content to index (Question and Answer)
     * @param questionAnswer The {@link QuestionAnswer} to index
     * @param plugin The {@link Plugin}
     * @return The content to index
     */
    private static String getContentToIndex( Announce announce, Plugin plugin )
    {
        StringBuffer sbContentToIndex = new StringBuffer(  );
        //Do not index question here
        sbContentToIndex.append( announce.getTitle(  ) );
        sbContentToIndex.append( BLANK );
        sbContentToIndex.append( announce.getDescription(  ) );
        sbContentToIndex.append( BLANK );
        sbContentToIndex.append( announce.getTags(  ) );

        return sbContentToIndex.toString(  );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResourceUid( String strResourceId, String strResourceType )
    {
    	StringBuffer sb = new StringBuffer( strResourceId );
    	sb.append( SolrConstants.CONSTANT_UNDERSCORE ).append( SHORT_NAME );
        
    	return sb.toString(  );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getResourcesName(  )
    {
        return LIST_RESSOURCES_NAME;
    }
}
