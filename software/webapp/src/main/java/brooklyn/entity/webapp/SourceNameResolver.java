package brooklyn.entity.webapp;



public class SourceNameResolver {

    public String getNameOfRepositoryGitFromHttpsUrl(String url){
        String nameOfRepository="";
        nameOfRepository=url.substring(url.lastIndexOf("/")+1, url.lastIndexOf("."));
        return nameOfRepository;
    }

    /**
     * Return the id of the resource pointed by the url without extension
     * E.g. http://example.com/resource.tar return resource
     * @param url
     * @return
     */

    public String getIdOfTarballFromUrl(String url){
        String nameOfTarballResource= getTarballResourceNameFromUrl(url);
        return nameOfTarballResource.substring(0,nameOfTarballResource.indexOf("."));
    }

    /**
     * Return the name of the tarball resource.
     * E.g. http://example.com/resource.tar return resource.tar
     * @param url
     * @return
     */
    public String getTarballResourceNameFromUrl(String url) {
        String resourceName = url.substring(url.lastIndexOf('/') + 1);
        return resourceName;
    }





}
