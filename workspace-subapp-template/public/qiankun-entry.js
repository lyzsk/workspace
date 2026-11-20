(function () {
  var moduleUrl = '/ies/micro/subapp-micro.js'
  var ready = import(/* @vite-ignore */ moduleUrl)

  function call(name, props) {
    return ready.then(function (mod) {
      return mod[name](props)
    })
  }

  var lifecycles = {
    bootstrap: function (props) {
      return call('bootstrap', props)
    },
    mount: function (props) {
      return call('mount', props)
    },
    unmount: function (props) {
      return call('unmount', props)
    },
    update: function (props) {
      return call('update', props)
    }
  }

  window['workspace:ies'] = lifecycles
  window['ies'] = lifecycles
})()
