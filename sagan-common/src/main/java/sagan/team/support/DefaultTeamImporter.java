package sagan.team.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.social.github.api.GitHub;
import org.springframework.social.github.api.GitHubUser;
import org.springframework.social.github.api.impl.GitHubTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultTeamImporter implements TeamImporter {

    private static final String API_URL_BASE = "https://api.github.com";

    private final TeamService teamService;
    private final String gitHubTeamId;
    private final GitHubTemplate gitHub;

    @Autowired
    public DefaultTeamImporter(TeamService teamService, @Value("${github.team.id}") String gitHubTeamId,
                               @Value("${github.access.token}") String gitHubAccessToken) {
        this.teamService = teamService;
        this.gitHubTeamId = gitHubTeamId;
        this.gitHub = new GitHubTemplate(gitHubAccessToken);
    }

    @Transactional
    public void importTeamMembers() {
        GitHubUser[] users = getGitHubUsers(this.gitHub);
        List<Long> userIds = new ArrayList<>();
        for (GitHubUser user : users) {
            userIds.add(user.getId());
            String userName = getNameForUser(user.getLogin(), this.gitHub);

            this.teamService.createOrUpdateMemberProfile(user.getId(), user.getLogin(), user.getAvatarUrl(), userName);
        }
        this.teamService.showOnlyTeamMembersWithIds(userIds);
    }

    private GitHubUser[] getGitHubUsers(GitHub gitHub) {
        String membersUrl = API_URL_BASE + "/teams/{teamId}/members?per_page=100";
        ResponseEntity<GitHubUser[]> entity =
                gitHub.restOperations().getForEntity(membersUrl, GitHubUser[].class, gitHubTeamId);
        return entity.getBody();
    }

    public String getNameForUser(String username, GitHub gitHub) {
        return gitHub.restOperations()
                .getForObject(API_URL_BASE + "/users/{user}", GitHubUser.class, username)
                .getName();
    }
}
