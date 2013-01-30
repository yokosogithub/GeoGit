<html>
<head>
<title> GeoGit Web API </title>
<meta name="ROBOTS" content="NOINDEX, NOFOLLOW"/>
</head>
<body>
<h2>Geogit repositories</h2>
<#if repositories?size != 0>
<ul>
<#foreach repo in repositories>
<li><a href="${page.pageURI(repo)}">${repo}</a></li>
</#foreach>
</ul>
<#else>
<p>There are no REST extensions installed.  If you expected some, please verify your installation (did you restart the server?).</p>
</#if>
</body>
</html>
