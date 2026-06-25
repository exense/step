This is still very early Work in progress, so some of it is quick and dirty to at least get an initial prototype to work.

Basically, it's a modified and stripped down version of the step controller, meant to be executed lika a local application.
Technically, it still consists of a backend and frontend. For development, they will be separated (as they are for the main Step product),
so you'll also need to launch both of them separately.

To start the frontend, check out branch SED-4429-step-ap-ide of the step-frontend-workspace project, then run
npm run serve:os:cli:local . For a little more information, see the SED-4557 ticket.

To start the backend and use the application, start the StepUp main class. This technically launches the actual controller,
and a client app that is basically a browser. Once started, you can also use a regular browser pointed to http://localhost:4201 .

