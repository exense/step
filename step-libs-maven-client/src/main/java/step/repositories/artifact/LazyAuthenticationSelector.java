package step.repositories.artifact;

import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;

public class LazyAuthenticationSelector implements AuthenticationSelector
{
    private final MirrorSelector mirrorSelector;
    private final DefaultAuthenticationSelector defaultAuthSelector;

    LazyAuthenticationSelector(MirrorSelector mirrorSelector)
    {
        this.mirrorSelector = mirrorSelector;
        this.defaultAuthSelector = new DefaultAuthenticationSelector();
    }

    @Override
    public Authentication getAuthentication(RemoteRepository repository)
    {
        RemoteRepository mirror = mirrorSelector.getMirror(repository);
        if (mirror != null)
        {
            return defaultAuthSelector.getAuthentication(mirror);
        }
        return defaultAuthSelector.getAuthentication(repository);
    }

    public void add(String id, Authentication authentication)
    {
        defaultAuthSelector.add(id, authentication);
    }
}
