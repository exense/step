## What is Step?

Step is a full-featured orchestration platform for software automation. It unifies the software automation disciplines and enables the reuse of automation artifacts through the whole DevOps lifecycle, from large scale test automation to load & performance tests, RPA and monitoring.

It provides all the means to make the most of existing automation artefacts by enabling their reusability over different automation tasks and teams on one centralized platform, and thus reduce duplication of code, tools and resources over projects:
![Illustration of Steps reusability](https://step.dev/knowledgebase/images/step-reusability.svg)

Step focuses on providing a unique platform for the orchestration of multi-purpose scalable automation scenarios while integrating existing automation tools (mainly test automation tools) and DevOps platforms:

![Illustration of Steps integration](https://step.dev/knowledgebase/images/step-integration.svg)

## How to start with Step?

The simplest and most efficient way to start using Step is to [get a SaaS instance](https://portal.stepcloud.ch) and follow one of the **[getting started tutorials](https://step.dev/all-resources?tab=tutorials-tab)**. Simply select the one that best fits your use-case. 

If you prefer installing your own cluster, follow the [Quick setup guide](https://step.dev/knowledgebase/setup/installation/binaries/quick-setup), alternatively you can use our OpenSource code to build your own version of Step.

## Important links

**Documentation** available at : [https://step.dev/knowledgebase](https://step.dev/knowledgebase/documentation)

**Official Website** : [http://step.dev](http://step.dev)

**Download link** : [step-distribution/releases](https://github.com/exense/step-distribution/releases)

**Step FrontEnd**: [https://github.com/exense/step-frontend](https://github.com/exense/step-frontend)

## Instructions for developers

### Git hooks setup

After cloning the project, issue the following command to set up the git hooks (git version >= 2.9 required):

```
git config core.hooksPath git-hooks
```
