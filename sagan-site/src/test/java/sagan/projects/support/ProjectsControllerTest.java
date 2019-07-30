package sagan.projects.support;

import sagan.SaganProfiles;
import sagan.projects.Project;
import sagan.projects.ProjectRelease;
import sagan.site.guides.GettingStartedGuides;
import sagan.site.guides.GuideHeader;
import sagan.site.guides.Topicals;
import sagan.site.guides.Tutorials;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static sagan.projects.ProjectRelease.ReleaseStatus.*;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers=ProjectsController.class)
@ActiveProfiles(SaganProfiles.STANDALONE)
public class ProjectsControllerTest {
    @MockBean
    private ProjectMetadataService projectMetadataService;

    @MockBean
    private GettingStartedGuides projectGuidesRepo;

    @MockBean
    private Tutorials projectTutorialRepo;

    @MockBean
    private Topicals projectTopicalRepo;

    @MockBean
    private OAuth2UserService<?,?> userService;

    @MockBean
    private ClientRegistrationRepository registrationRepository;
    
    private ProjectRelease currentRelease;

    private ProjectRelease anotherCurrentRelease;

    private ProjectRelease snapshotRelease;

    private List<ProjectRelease> releases;

    private Project springBoot;

    private Project springData;

    private Project springDataJpa;

    @Autowired
    private MockMvc mvc;

    @Before
    public void setUp() throws Exception {
        this.currentRelease = new ProjectRelease("2.1.0.RELEASE", GENERAL_AVAILABILITY,
                true, "", "", "", "");
        this.anotherCurrentRelease = new ProjectRelease("2.0.0.RELEASE", GENERAL_AVAILABILITY,
                true, "", "", "", "");
        this.snapshotRelease = new ProjectRelease("2.2.0.SNAPSHOT", SNAPSHOT,
                false, "", "", "", "");
        this.releases = Arrays.asList(currentRelease, anotherCurrentRelease, snapshotRelease);

        this.springBoot = new Project("spring-boot", "Spring Boot",
                "https://github.com/spring-projects/spring-boot", "/project/spring-boot", 0,
                releases, "project", "spring-boot", "");

        this.springData = new Project("spring-data", "Spring Data",
                "https://github.com/spring-projects/spring-data", "/project/spring-data", 0,
                releases, "project", "spring-data,spring-data-commons", "");
        this.springDataJpa = new Project("spring-data-jpa", "Spring Data JPA",
                "https://github.com/spring-projects/spring-data-jpa", "/project/spring-data-jpa", 0,
                releases, "project", "spring-data-jpa", "");

        this.springData.setChildProjectList(Arrays.asList(this.springDataJpa));
        this.springDataJpa.setParentProject(this.springData);

        GuideHeader[] guides = new GuideHeader[] {};
        given(this.projectGuidesRepo.findByProject(any())).willReturn(guides);
        given(this.projectTopicalRepo.findByProject(any())).willReturn(guides);
        given(this.projectTutorialRepo.findByProject(any())).willReturn(guides);

        given(projectMetadataService.getProjects()).willReturn(Arrays.asList(this.springBoot, this.springData));
        given(projectMetadataService.getProject("spring-boot")).willReturn(this.springBoot);
        given(projectMetadataService.getProject("spring-data")).willReturn(this.springData);
        given(projectMetadataService.getActiveTopLevelProjects()).willReturn(Arrays.asList(this.springBoot,
                this.springData));
    }

    @Test
    public void showProjectModelHasProjectData() throws Exception {
        this.mvc.perform(get("/projects/spring-boot"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedProject", this.springBoot))
                .andExpect(model().attribute("projects", Matchers.contains(this.springBoot, this.springData)))
                .andExpect(model().attribute("projectStackOverflow",
                        "https://stackoverflow.com/questions/tagged/spring-boot"));
    }

    @Test
    public void showProjectHasStackOverflowLink() throws Exception {
        this.mvc.perform(get("/projects/spring-data"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedProject", this.springData))
                .andExpect(model().attribute("projects", Matchers.contains(this.springBoot, this.springData)))
                .andExpect(model().attribute("projectStackOverflow",
                        "https://stackoverflow.com/questions/tagged/spring-data+or+spring-data-commons"));
    }

    @Test
    public void showAllProjects() throws Exception {
        this.mvc.perform(get("/projects"))
                .andExpect(status().isOk());
    }

    @Test
    public void showProjectHasReleases() throws Exception {
        this.mvc.perform(get("/projects/spring-boot"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentRelease", Optional.of(this.currentRelease)))
                .andExpect(model().attribute("otherReleases", Matchers.hasItems(this.anotherCurrentRelease,
                        this.snapshotRelease)));
    }

    @Test
    public void showProjectWithoutReleases() throws Exception {
        Project projectWithoutReleases =
                new Project("spring-norelease", "spring-norelease", "http://example.com",
                        "/project/spring-norelease", 0, Collections.emptyList(),
                        "project", "spring-example", "");
        when(projectMetadataService.getProject("spring-norelease")).thenReturn(projectWithoutReleases);

        this.mvc.perform(get("/projects/spring-norelease"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentRelease", Optional.empty()))
                .andExpect(model().attribute("otherReleases", Matchers.empty()));
    }

    @Test
    public void listProjectsProvidesProjectMetadata() throws Exception {
        this.mvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("springboot", this.springBoot))
                .andExpect(model().attribute("springdata", this.springData));
    }

}
