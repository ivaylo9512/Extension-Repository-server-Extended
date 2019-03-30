package com.tick42.quicksilver.services;

import com.tick42.quicksilver.exceptions.*;
import com.tick42.quicksilver.models.*;
import com.tick42.quicksilver.models.DTO.ExtensionDTO;
import com.tick42.quicksilver.models.DTO.PageDTO;
import com.tick42.quicksilver.models.Spec.ExtensionSpec;
import com.tick42.quicksilver.repositories.base.ExtensionRepository;
import com.tick42.quicksilver.repositories.base.UserRepository;
import com.tick42.quicksilver.services.base.GitHubService;
import com.tick42.quicksilver.services.base.TagService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.validation.constraints.Null;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionServiceImplTests {
    @Mock
    private ExtensionRepository extensionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TagService tagService;

    @Mock
    private GitHubService gitHubService;

    @InjectMocks
    private ExtensionServiceImpl extensionService;

    @Test
    public void create_whenUserExists_returnExtensionDTO() {
        //Arrange
        int userId = 1;
        Date uploadTime = new Date();
        Date commitTime = new Date();

        ExtensionSpec extensionSpec = new ExtensionSpec();
        extensionSpec.setName("name");
        extensionSpec.setVersion("1.0");
        extensionSpec.setDescription("description");
        extensionSpec.setGithub("gitHubLink");
        extensionSpec.setTags("tag1, tag2");

        UserModel userModel = new UserModel();
        userModel.setUsername("username");
        userModel.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userModel));

        Set<Tag> tags = new HashSet<>(Arrays.asList(new Tag("tag1"), new Tag("tag2")));
        when(tagService.generateTags(extensionSpec.getTags())).thenReturn(tags);

        GitHubModel github = new GitHubModel();
        github.setLastCommit(commitTime);
        github.setPullRequests(10);
        github.setOpenIssues(20);
        github.setLink("gitHubLink");
        when(gitHubService.generateGitHub(extensionSpec.getGithub())).thenReturn(github);

        Extension extension = new Extension();
        extension.setName("name");
        extension.setVersion("1.0");
        extension.setDescription("description");
        extension.isPending(true);
        extension.setOwner(userModel);
        extension.setUploadDate(uploadTime);
        extension.setTags(tags);
        extension.setGithub(github);

        ExtensionDTO expectedExtensionDTO = new ExtensionDTO();
        expectedExtensionDTO.setName("name");
        expectedExtensionDTO.setVersion("1.0");
        expectedExtensionDTO.setDescription("description");
        expectedExtensionDTO.setPending(true);
        expectedExtensionDTO.setUploadDate(uploadTime);
        expectedExtensionDTO.setOwnerName("username");
        expectedExtensionDTO.setOwnerId(1);
        expectedExtensionDTO.setUploadDate(uploadTime);
        expectedExtensionDTO.setTags(Arrays.asList("tag1", "tag2"));
        expectedExtensionDTO.setGitHubLink("gitHubLink");
        expectedExtensionDTO.setLastCommit(commitTime);
        expectedExtensionDTO.setPullRequests(10);
        expectedExtensionDTO.setOpenIssues(20);
        expectedExtensionDTO.setFeatured(false);
        expectedExtensionDTO.setTimesDownloaded(0);
        expectedExtensionDTO.setVersion("1.0");

        when(extensionRepository.save(any(Extension.class))).thenReturn(extension);

        //Act
        ExtensionDTO createdExtensionDTO = extensionService.create(extensionSpec, userId);
        createdExtensionDTO.getTags().sort(String::compareTo);

        //Assert
        Assert.assertThat(expectedExtensionDTO, samePropertyValuesAs(createdExtensionDTO));
    }

    @Test
    public void setFeaturedState_whenSetToFeatured_returnFeaturedExtensionDTO() {
        // Arrange
        Extension extension = new Extension();
        extension.isFeatured(false);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        ExtensionDTO extensionShouldBeFeatured = extensionService.setFeaturedState(1, "feature");

        //Assert
        Assert.assertTrue(extensionShouldBeFeatured.isFeatured());
    }

    @Test
    public void setFeaturedState_whenSetToUnfeatured_returnUnfeaturedExtensionDTO() {
        // Arrange
        Extension extension = new Extension();
        extension.isFeatured(true);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        ExtensionDTO extensionShouldBeUnfeatured = extensionService.setFeaturedState(1, "unfeature");

        //Assert
        Assert.assertFalse(extensionShouldBeUnfeatured.isFeatured());
    }

    @Test(expected = InvalidStateException.class)
    public void setFeaturedState_whenGivenInvalidParameter_shouldThrow() {
        // Arrange
        Extension extension = new Extension();
        extension.isFeatured(true);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        ExtensionDTO extensionShouldThrow = extensionService.setFeaturedState(1, "wrongString");

        //Assert
    }

    @Test
    public void setPublishedState_whenSetToPublished_returnPublishedExtensionDTO() {
        // Arrange
        Extension extension = new Extension();
        extension.isPending(true);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        ExtensionDTO extensionShouldBePending = extensionService.setPublishedState(1, "publish");

        //Assert
        Assert.assertFalse(extensionShouldBePending.isPending());
    }

    @Test
    public void setPublishedState_whenSetToUnpublished_returnUnpublishedExtensionDTO() {
        // Arrange
        Extension extension = new Extension();
        extension.isPending(false);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        ExtensionDTO extensionShouldBeUnpublished = extensionService.setPublishedState(1, "unpublish");

        //Assert
        Assert.assertTrue(extensionShouldBeUnpublished.isPending());
    }

    @Test(expected = InvalidStateException.class)
    public void setPublishedState_whenGivenInvalidParameter_shouldThrow() {
        // Arrange
        Extension extension = new Extension();
        extension.isFeatured(true);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        ExtensionDTO extensionShouldThrow = extensionService.setPublishedState(1, "wrongString");

        //Assert
    }

    @Test
    public void findPending_shouldReturnListOfPendingExtensionDTOs() {
        //Arrange
        Extension extension1 = new Extension();
        Extension extension2 = new Extension();
        extension1.isPending(true);
        extension2.isPending(true);
        List<Extension> extensions = Arrays.asList(extension1, extension2);

        when(extensionRepository.findByPending(true)).thenReturn(extensions);

        //Act
        List<ExtensionDTO> pendingExtensionDTOs = extensionService.findPending();

        //Assert
        Assert.assertEquals(2, pendingExtensionDTOs.size());
        Assert.assertTrue(pendingExtensionDTOs.get(0).isPending());
        Assert.assertTrue(pendingExtensionDTOs.get(1).isPending());
    }

    @Test
    public void findFeatured_shouldReturnListOfFeaturedExtensionDTOs() {
        //Arrange
        Extension extension1 = new Extension();
        Extension extension2 = new Extension();
        extension1.isFeatured(true);
        extension2.isFeatured(true);
        List<Extension> extensions = Arrays.asList(extension1, extension2);

        when(extensionRepository.findByFeatured(true)).thenReturn(extensions);

        //Act
        List<ExtensionDTO> featuredExtensionDTOs = extensionService.findFeatured();

        //Assert
        Assert.assertEquals(2, featuredExtensionDTOs.size());
        Assert.assertTrue(featuredExtensionDTOs.get(0).isFeatured());
        Assert.assertTrue(featuredExtensionDTOs.get(1).isFeatured());
    }

    @Test(expected = NullPointerException.class)
    public void findById_whenExtensionDoesntExist_shouldThrow() {
        //Arrange
        UserDetails user = new UserDetails("text", "test", new ArrayList<>(), 1);
        when(extensionRepository.findById(1)).thenReturn(null);

        //Act
        extensionService.findById(1, user);
    }

    @Test(expected = ExtensionUnavailableException.class)
    public void findById_whenOwnerIsInactiveaAndUserIsNull_shouldThrow() {
        //Arrange

        UserModel owner = new UserModel();
        owner.setIsActive(false);

        Extension extension = new Extension();
        extension.setOwner(owner);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        extensionService.findById(1, null);
    }

    @Test(expected = ExtensionUnavailableException.class)
    public void findById_whenOwnerIsInactiveAndUserIsNotAdmin_shouldThrow() {
        //Arrange
        Collection<GrantedAuthority> authorities = new ArrayList<>(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        UserDetails user = new UserDetails("TEST", "TEST", authorities, 1);

        UserModel owner = new UserModel();
        owner.setIsActive(false);

        Extension extension = new Extension();
        extension.setOwner(owner);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        extensionService.findById(1, user);
    }

    @Test(expected = ExtensionUnavailableException.class)
    public void findById_whenExtensionIsPendingAndOwnerIsActiveAndUserIsNull_shouldThrow() {
        //Arrange
        UserModel owner = new UserModel();
        owner.setIsActive(true);

        Extension extension = new Extension();
        extension.isPending(true);
        extension.setOwner(owner);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        extensionService.findById(1, null);
    }

    @Test(expected = ExtensionUnavailableException.class)
    public void findById_whenExtensionIsPendingAndOwnerIsActiveAndUserIsNotOwnerAndNotAdmin_shouldThrow() {
        //Arrange
        Collection<GrantedAuthority> authorities = new ArrayList<>(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        UserDetails user = new UserDetails("TEST", "TEST", authorities, 1);

        UserModel owner = new UserModel();
        owner.setIsActive(true);
        owner.setId(2);

        Extension extension = new Extension();
        extension.isPending(true);
        extension.setOwner(owner);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        extensionService.findById(1, user);
    }

    @Test
    public void findById_whenExtensionIsPendingAndOwnerIsInactiveAndUserIsAdmin_shouldReturnExtensionDTO() {
        //Arrange
        Collection<GrantedAuthority> authorities = new ArrayList<>(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        UserDetails user = new UserDetails("TEST", "TEST", authorities, 1);

        UserModel owner = new UserModel();
        owner.setIsActive(false);

        Extension extension = new Extension();
        extension.setId(1);
        extension.isPending(true);
        extension.setOwner(owner);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        ExtensionDTO expectedExtensionDTO = extensionService.findById(1, user);

        //Assert
        Assert.assertEquals(extension.getId(), expectedExtensionDTO.getId());
    }

    @Test
    public void findById_whenExtensionIsPendingAndOwnerIsActiveAndUserIsNotAdmin_shouldReturnExtensionDTO() {
        //Arrange
        Collection<GrantedAuthority> authorities = new ArrayList<>(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        UserDetails user = new UserDetails("TEST", "TEST", authorities, 1);

        UserModel owner = new UserModel();
        owner.setIsActive(true);
        owner.setId(1);

        Extension extension = new Extension();
        extension.setId(1);
        extension.isPending(true);
        extension.setOwner(owner);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        ExtensionDTO expectedExtensionDTO = extensionService.findById(1, user);

        //Assert
        Assert.assertEquals(extension.getId(), expectedExtensionDTO.getId());
    }

    @Test
    public void findById_whenExtensionIsNotPendingAndOwnerIsActiveAndUserIsNotOwnerAndNotAdmin_shouldReturnExtensionDTO() {
        //Arrange
        Collection<GrantedAuthority> authorities = new ArrayList<>(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        UserDetails user = new UserDetails("TEST", "TEST", authorities, 1);

        UserModel owner = new UserModel();
        owner.setIsActive(true);
        owner.setId(2);

        Extension extension = new Extension();
        extension.setId(1);
        extension.isPending(false);
        extension.setOwner(owner);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        ExtensionDTO expectedExtensionDTO = extensionService.findById(1, user);

        //Assert
        Assert.assertEquals(extension.getId(), expectedExtensionDTO.getId());
    }

    @Test(expected = NullPointerException.class)
    public void update_whenExtensionDoesntExist_ShouldThrow() {
        //Assert
        when(extensionRepository.findById(1)).thenReturn(null);

        //Act
        extensionService.update(1, new ExtensionSpec(), 1);
    }

    @Test(expected = NullPointerException.class)
    public void update_whenUserDoesntExist_ShouldThrow() {
        //Assert
        Extension extension = new Extension();

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));
        when(userRepository.findById(1)).thenReturn(null);

        //Act
        extensionService.update(1, new ExtensionSpec(), 1);
    }

    @Test(expected = UnauthorizedExtensionModificationException.class)
    public void update_whenUserIsNotOwnerAndNotAdmin_ShouldThrow() {
        //Assert
        UserModel userModel = new UserModel();
        userModel.setId(1);
        userModel.setRole("ROLE_USER");

        UserModel owner = new UserModel();
        owner.setId(2);

        Extension extension = new Extension();
        extension.setOwner(owner);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));
        when(userRepository.findById(1)).thenReturn(Optional.of(userModel));

        //Act
        extensionService.update(1, new ExtensionSpec(), 1);
    }

    @Test
    public void update_whenUserIsOwner_returnUpdatedExtensionDTO() {
        //Assert
        Date commitTime = new Date();

        UserModel userModel = new UserModel();
        userModel.setId(1);
        userModel.setRole("ROLE_USER");

        UserModel owner = new UserModel();
        owner.setId(1);

        ExtensionSpec extensionSpec = new ExtensionSpec();
        extensionSpec.setName("newName");
        extensionSpec.setVersion("1.0");
        extensionSpec.setDescription("description");
        extensionSpec.setGithub("gitHubLink");
        extensionSpec.setTags("tag1, tag2");

        Set<Tag> tags = new HashSet<>(Arrays.asList(new Tag("tag1"), new Tag("tag2")));
        when(tagService.generateTags(extensionSpec.getTags())).thenReturn(tags);

        GitHubModel github = new GitHubModel();
        github.setLastCommit(commitTime);
        github.setPullRequests(10);
        github.setOpenIssues(20);
        github.setLink("gitHubLink");
        when(gitHubService.generateGitHub(extensionSpec.getGithub())).thenReturn(github);

        Extension extension = new Extension();
        extension.setOwner(owner);
        extension.setName("oldName");
        extension.setVersion("1.0");
        extension.setDescription("description");
        extension.setGithub(github);
        extension.setTags(tags);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));
        when(userRepository.findById(1)).thenReturn(Optional.of(userModel));

        //Act
        ExtensionDTO actualExtensionDTO = extensionService.update(1, extensionSpec, 1);

        //Assert
        Assert.assertEquals(actualExtensionDTO.getName(), "newName");
    }

    @Test
    public void update_whenUserIsAdmin_returnUpdatedExtensionDTO() {
        //Assert
        Date commitTime = new Date();

        UserModel userModel = new UserModel();
        userModel.setId(2);
        userModel.setRole("ROLE_ADMIN");

        UserModel owner = new UserModel();
        owner.setId(1);

        ExtensionSpec extensionSpec = new ExtensionSpec();
        extensionSpec.setName("newName");
        extensionSpec.setVersion("1.0");
        extensionSpec.setDescription("description");
        extensionSpec.setGithub("gitHubLink");
        extensionSpec.setTags("tag1, tag2");

        Set<Tag> tags = new HashSet<>(Arrays.asList(new Tag("tag1"), new Tag("tag2")));
        when(tagService.generateTags(extensionSpec.getTags())).thenReturn(tags);

        GitHubModel github = new GitHubModel();
        github.setLastCommit(commitTime);
        github.setPullRequests(10);
        github.setOpenIssues(20);
        github.setLink("gitHubLink");
        when(gitHubService.generateGitHub(extensionSpec.getGithub())).thenReturn(github);

        Extension extension = new Extension();
        extension.setOwner(owner);
        extension.setName("oldName");
        extension.setVersion("1.0");
        extension.setDescription("description");
        extension.setGithub(github);
        extension.setTags(tags);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));
        when(userRepository.findById(1)).thenReturn(Optional.of(userModel));

        //Act
        ExtensionDTO actualExtensionDTO = extensionService.update(1, extensionSpec, 1);

        //Assert
        Assert.assertEquals(actualExtensionDTO.getName(), "newName");
    }

    @Test(expected = NullPointerException.class)
    public void delete_whenExtensionDoesntExist_ShouldThrow() {
        //Assert
        when(extensionRepository.findById(1)).thenReturn(null);

        //Act
        extensionService.delete(1, 1);
    }

    @Test(expected = NullPointerException.class)
    public void delete_whenUserDoesntExist_ShouldThrow() {
        //Assert
        Extension extension = new Extension();

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));
        when(userRepository.findById(1)).thenReturn(null);

        //Act
        extensionService.delete(1, 1);
    }

    @Test
    public void delete_whenUserIsOwner_ShouldNotThrow() {
        //Assert
        UserModel userModel = new UserModel();
        userModel.setId(1);
        userModel.setRole("ROLE_USER");

        UserModel owner = new UserModel();
        owner.setId(1);

        Extension extension = new Extension();
        extension.setOwner(owner);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));
        when(userRepository.findById(1)).thenReturn(Optional.of(userModel));

        //Act
        try {
            extensionService.delete(1, 1);
            Assert.assertTrue(Boolean.TRUE);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void delete_whenUserIsAdmin_ShouldNotThrow() {
        //Assert
        UserModel userModel = new UserModel();
        userModel.setId(1);
        userModel.setRole("ROLE_ADMIN");

        UserModel owner = new UserModel();
        owner.setId(2);

        Extension extension = new Extension();
        extension.setOwner(owner);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));
        when(userRepository.findById(1)).thenReturn(Optional.of(userModel));

        //Act
        try {
            extensionService.delete(1, 1);
            Assert.assertTrue(Boolean.TRUE);
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test(expected = InvalidParameterException.class)
    public void findAll_whenPageMoreThanTotalPages_shouldThrow() {
        //Arrange
        String name = "name";
        int page = 5;
        int perPage = 10;
        Long totalResults = 21L;

        when(extensionRepository.getTotalResults(name)).thenReturn(totalResults);

        //Act
        extensionService.findAll(name, "date", page, perPage);
    }

    @Test
    public void findAll_whenPageMoreThanTotalPagesAndTotalResultsAreZero_shouldNotThrow() {
        //Arrange
        String name = "name";
        int page = 5;
        int perPage = 10;
        Long totalResults = 0L;

        when(extensionRepository.getTotalResults(name)).thenReturn(totalResults);

        //Act
        try {
            extensionService.findAll(name, "date", page, perPage);
            Assert.assertTrue(Boolean.TRUE);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test(expected = InvalidParameterException.class)
    public void findAll_whenInvalidParameter_shouldThrow() {
        //Arrange
        String name = "name";
        String orderBy = "orderType";
        int page = 5;
        int perPage = 10;
        Long totalResults = 500L;

        when(extensionRepository.getTotalResults(name)).thenReturn(totalResults);

        //Act
        extensionService.findAll(name, orderBy, page, perPage);
    }


    @Test
    public void delete_whenExtensionsFound_shouldInvokeDeleteInRepository() {
        //Arrange
        UserModel userModel = new UserModel();
        Extension extension = new Extension();
        extension.setOwner(userModel);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));
        when(userRepository.findById(1)).thenReturn(Optional.of(userModel));
        doNothing().when(extensionRepository).delete(isA(Extension.class));

        extensionService.delete(1, 1);
        verify(extensionRepository, times(1)).delete(extension);
    }

    @Test(expected = NullPointerException.class)
    public void increaseDownloadCount_whenExtensionDoesntExist_shouldThrow() {
        when(extensionRepository.findById(1)).thenReturn(null);

        //Act
        extensionService.increaseDownloadCount(1);
    }

    @Test(expected = ExtensionUnavailableException.class)
    public void increaseDownloadCount_whenExtensionIsPending_shouldThrow() {
        Extension extension = new Extension();
        extension.isPending(true);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        extensionService.increaseDownloadCount(1);
    }

    @Test(expected = ExtensionUnavailableException.class)
    public void increaseDownloadCount_whenOwnerIsDeactivated_shouldThrow() {
        UserModel owner = new UserModel();
        owner.setIsActive(false);
        Extension extension = new Extension();
        extension.setOwner(owner);
        extension.isPending(false);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        //Act
        extensionService.increaseDownloadCount(1);
    }

    @Test
    public void increaseDownloadCount_whenExtensionAvailable_shouldIncreasseTimesDownlaoded() {
        int times = 1;
        UserModel owner = new UserModel();
        owner.setIsActive(true);
        Extension extension = new Extension();
        extension.setOwner(owner);
        extension.isPending(false);
        extension.setTimesDownloaded(times);

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));

        when(extensionRepository.save(extension)).thenReturn(extension);

        //Act
        ExtensionDTO result = extensionService.increaseDownloadCount(1);

        //Assert
        Assert.assertEquals(result.getTimesDownloaded(), times + 1);
    }

    @Test(expected = NullPointerException.class)
    public void fetchGitHub_whenExtensionDoesntExist_ShouldThrow() {
        //Arrange
        when(extensionRepository.findById(1)).thenReturn(null);

        //Act
        extensionService.fetchGitHub(1, 1);

    }

    @Test(expected = NullPointerException.class)
    public void fetchGitHub_whenUserDoesntExist_ShouldThrow() {
        //Arrange
        Extension extension = new Extension();
        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));
        when(userRepository.findById(1)).thenReturn(null);

        //Act
        extensionService.fetchGitHub(1, 1);

    }

    @Test(expected = UnauthorizedExtensionModificationException.class)
    public void fetchGitHub_whenUserIsNotAdmin_ShouldThrow() {
        //Arrange
        Extension extension = new Extension();
        extension.setId(1);
        UserModel userModel = new UserModel();
        userModel.setRole("USER");

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));
        when(userRepository.findById(1)).thenReturn(Optional.of(userModel));

        //Act
        extensionService.fetchGitHub(1, 1);

    }

    @Test
    public void fetchGitHub_whenEtensionIsAvailableAndUserIsAdmin_ShouldNotThrow() {
        //Arrange
        Extension extension = new Extension();
        UserModel userModel = new UserModel();
        userModel.setRole("ROLE_ADMIN");

        when(extensionRepository.findById(1)).thenReturn(Optional.of(extension));
        when(userRepository.findById(1)).thenReturn(Optional.of(userModel));

        try {
            extensionService.fetchGitHub(1, 1);
            Assert.assertTrue(Boolean.TRUE);
        } catch (Error e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void generateExtensionDTOList_whenGivenListOfExtension_returnListOfExtensionDTO() {
        //Arrange
        Extension extension1 = new Extension();
        Extension extension2 = new Extension();
        List<Extension> extensions = Arrays.asList(extension1, extension2);

        //Act
        List<ExtensionDTO> extensionDTOs = extensionService.generateExtensionDTOList(extensions);

        //Assert
        Assert.assertEquals(extensions.size(), extensionDTOs.size());
        Assert.assertNotNull(extensionDTOs.get(1));
    }
}
