# Contribution Guidelines

So you are wondering how you can contribute to Firefly? Congrats, you've landed on the right page!

The Firefly team welcomes interest from the entire astronomical community, from beginning users to those with deep experience in open source contributions. Examples of valuable contributions include:
- Stories of how you are using or want to use Firefly;
- User feedback, including ideas for new capabilities;
- Ideas for how to improve the documentation;
- Testing code written by others;
- Code for new capabilities

Below we provide some guidelines for how to contribute.

## How can I contribute?

There are multiple ways in which you can contribute:

### Reporting a Bug

Firefly is in active development. It's no surprise that you may encounter something that doesn't work for your use case. Or maybe you have some suggestions about how we can improve some functionality. Feel free to share any of it with us by [opening an issue](https://docs.github.com/en/github/managing-your-work-on-github/creating-an-issue) at [Firefly Github Issues](https://github.com/Caltech-IPAC/firefly/issues).

Please make sure that you provide all the necessary information, especially how to reproduce your problem — it will not only make our work easier but will also help you communicate your problem more effectively.

### Editing the Documentation

Due to the rapid pace of development and limited resources, Firefly's documentation may not always be up-to-date or fully comprehensive. And here lies an opportunity for you: you can edit the documentation stored as markdown text files (`*.md`) in the [`docs` directory](https://github.com/Caltech-IPAC/firefly/tree/dev/docs) of Firefly and as docstrings in the `src` code files.

You can see the Firefly JS docs at http://localhost:8080/firefly/docs/js/index.html if you have built them locally, as follows:
```shell
gradle firefly:buildJsDoc
gradle firefly:buildAndDeploy
```

After editing the markdown or source files, build the docs again to check if your changes render correctly. Then you can submit your changes to us by making a patch as described in the next section.

### Making a Patch (aka Code Contribution)

Currently, Firefly tracks development priorities and tasks through Jira, which is internal to us, rather than through public GitHub issues. As a result, we don't have "good-first" or "easy" labeled issues to invite code contributions from new contributors. However, that shouldn't discourage you from contributing!  As a new user, you have a better perspective on where our documentation lacks, and you can edit it to make it better, as described in the previous section.

If you've [set up Firefly locally](https://github.com/Caltech-IPAC/firefly/tree/dev?tab=readme-ov-file#setup) and have some familiarity with the codebase, we encourage you to contribute by adding features you'd find useful or fixing issues that affect your workflow. Your contributions will be invaluable to improve open-sourced Firefly for other users as well.

To contribute your code, you'll need to create a [pull request](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/about-pull-requests) from your fork of the Firefly repository. If this is your first time creating a pull request on GitHub, please refer to [this guide](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork).

When submitting a pull request, please provide a clear description of your changes, including any relevant testing instructions. Also, make sure your patch maintains code quality, is documented, and is testable.

### Spreading the Word of Mouth

If you find Firefly helpful, you can share it with your peers, colleagues, and anyone who can benefit from Firefly. If you've used Firefly in your research, please acknowledge us. By telling other people about how Firefly helped you, you'll help us in turn, extending Firefly's impact. And we would absolutely love it if you give us a shout-out on IPAC's social media: [LinkedIn](https://www.linkedin.com/showcase/ipac-at-caltech/) | [Twitter](https://x.com/caltechipac).

### Reporting user experiences

We love to hear from our users! We welcome GitHub issues about more than just bugs. We'd be happy for you to tell us: that you've used Firefly in a new application; about something that delighted you; about a particular piece of astronomical data that we don't handle well; about a workflow or UX that was awkward; or anything else you'd like to tell us about Firefly.  (Note that if you use Firefly as part of a specific astronomical archive or science platform, that organization may have its own way that it prefers to collect feedback for us, such as a help desk or users' committee.) 

## What if I need help?

We encourage you to ask for help without hesitation if you want to contribute to Firefly. Other than [opening a GitHub issue](https://github.com/Caltech-IPAC/firefly/issues) as mentioned above, you can also reach out to us by contacting the [IRSA Help Desk](https://irsa.ipac.caltech.edu/docs/help_desk.html).

---

Thank you for contributing! 

These contribution guidelines have been adapted from [TARDIS Contribution Guidelines](https://github.com/tardis-sn/tardis/blob/master/CONTRIBUTING.md).
