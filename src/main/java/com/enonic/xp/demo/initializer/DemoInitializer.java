package com.enonic.xp.demo.initializer;

import java.util.concurrent.Callable;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.xp.content.ApplyContentPermissionsParams;
import com.enonic.xp.content.Content;
import com.enonic.xp.content.ContentConstants;
import com.enonic.xp.content.ContentPath;
import com.enonic.xp.content.ContentService;
import com.enonic.xp.content.CreateContentParams;
import com.enonic.xp.content.UpdateContentParams;
import com.enonic.xp.context.Context;
import com.enonic.xp.context.ContextAccessor;
import com.enonic.xp.context.ContextBuilder;
import com.enonic.xp.data.PropertyTree;
import com.enonic.xp.export.ExportService;
import com.enonic.xp.export.ImportNodesParams;
import com.enonic.xp.export.NodeImportResult;
import com.enonic.xp.index.IndexService;
import com.enonic.xp.node.NodePath;
import com.enonic.xp.schema.content.ContentTypeName;
import com.enonic.xp.security.PrincipalKey;
import com.enonic.xp.security.RoleKeys;
import com.enonic.xp.security.User;
import com.enonic.xp.security.acl.AccessControlEntry;
import com.enonic.xp.security.acl.AccessControlList;
import com.enonic.xp.security.acl.Permission;
import com.enonic.xp.security.auth.AuthenticationInfo;
import com.enonic.xp.vfs.VirtualFile;
import com.enonic.xp.vfs.VirtualFiles;

@Component(immediate = true)
public final class DemoInitializer
{

    private static final AccessControlList PERMISSIONS =
        AccessControlList.of( AccessControlEntry.create().principal( PrincipalKey.ofAnonymous() ).allow( Permission.READ ).build(),
                              AccessControlEntry.create().principal( RoleKeys.EVERYONE ).allow( Permission.READ ).build(),
                              AccessControlEntry.create().principal( RoleKeys.AUTHENTICATED ).allowAll().build(),
                              AccessControlEntry.create().principal( RoleKeys.CONTENT_MANAGER_ADMIN ).allowAll().build() );

    private ContentService contentService;

    private ExportService exportService;

    private IndexService indexService;

    private final Logger LOG = LoggerFactory.getLogger( DemoInitializer.class );

    @Activate
    public void initialize()
        throws Exception
    {
        if ( this.indexService.isMaster() )
        {
            runAs( createInitContext(), () -> {
                doInitialize();
                return null;
            } );
        }
    }

    private Context createInitContext()
    {
        return ContextBuilder.from( ContextAccessor.current() ).
            authInfo( AuthenticationInfo.create().principals( RoleKeys.CONTENT_MANAGER_ADMIN ).user( User.ANONYMOUS ).build() ).
            branch( ContentConstants.BRANCH_DRAFT ).
            repositoryId( ContentConstants.CONTENT_REPO.getId() ).
            build();
    }

    private void doInitialize()
        throws Exception
    {
        final ContentPath demoSitePath = ContentPath.from( "/my-corporation" );
        if ( hasContent( demoSitePath ) )
        {
            return;
        }

        final Bundle bundle = FrameworkUtil.getBundle( this.getClass() );

        final VirtualFile source = VirtualFiles.from( bundle, "/import" );

        final NodeImportResult nodeImportResult = this.exportService.importNodes( ImportNodesParams.create().
            source( source ).
            targetNodePath( NodePath.create( "/content" ).build() ).
            includeNodeIds( true ).
            dryRun( false ).
            build() );

        logImport( nodeImportResult );

        createLargeTree();

        // set permissions
        final Content demoContent = contentService.getByPath( demoSitePath );
        if ( demoContent != null )
        {
            final UpdateContentParams setFeaturesPermissions = new UpdateContentParams().
                contentId( demoContent.getId() ).
                editor( ( content ) -> {
                    content.permissions = PERMISSIONS;
                    content.inheritPermissions = false;
                } );
            contentService.update( setFeaturesPermissions );

            contentService.applyPermissions( ApplyContentPermissionsParams.create().
                contentId( demoContent.getId() ).
                build() );
        }
    }

    private void logImport( final NodeImportResult nodeImportResult )
    {
        LOG.info( "-------------------" );
        LOG.info( "Imported nodes:" );
        for ( final NodePath nodePath : nodeImportResult.getAddedNodes() )
        {
            LOG.info( nodePath.toString() );
        }

        LOG.info( "-------------------" );
        LOG.info( "Binaries:" );
        nodeImportResult.getExportedBinaries().forEach( LOG::info );

        LOG.info( "-------------------" );
        LOG.info( "Errors:" );
        for ( final NodeImportResult.ImportError importError : nodeImportResult.getImportErrors() )
        {
            LOG.info( importError.getMessage(), importError.getException() );
        }
    }

    private CreateContentParams.Builder makeFolder()
    {
        return CreateContentParams.create().
            owner( PrincipalKey.ofAnonymous() ).
            contentData( new PropertyTree() ).
            type( ContentTypeName.folder() ).
            inheritPermissions( true );
    }

    private boolean hasContent( final ContentPath path )
    {
        try
        {
            return this.contentService.getByPath( path ) != null;
        }
        catch ( final Exception e )
        {
            return false;
        }
    }

    @Reference
    public void setExportService( final ExportService exportService )
    {
        this.exportService = exportService;
    }

    @Reference
    public void setContentService( final ContentService contentService )
    {
        this.contentService = contentService;
    }

    @Reference
    public void setIndexService( final IndexService indexService )
    {
        this.indexService = indexService;
    }

    private void createLargeTree()
    {
        final ContentPath largeTreePath = ContentPath.from( "/large-tree" );
        if ( !hasContent( largeTreePath ) )
        {
            contentService.create( makeFolder().
                name( "large-tree" ).
                displayName( "Large tree" ).
                parent( ContentPath.ROOT ).
                permissions( PERMISSIONS ).
                inheritPermissions( false ).
                build() );

            for ( int i = 1; i <= 2; i++ )
            {
                Content parent = contentService.create( makeFolder().
                    displayName( "large-tree-node-" + i ).
                    displayName( "Large tree node " + i ).
                    parent( largeTreePath ).build() );

                for ( int j = 1; j <= 100; j++ )
                {
                    contentService.create( makeFolder().
                        displayName( "large-tree-node-" + i + "-" + j ).
                        displayName( "Large tree node " + i + "-" + j ).
                        parent( parent.getPath() ).build() );
                }
            }
        }
    }

    private <T> T runAs( final Context context, final Callable<T> runnable )
    {
        return context.callWith( runnable );
    }
}
