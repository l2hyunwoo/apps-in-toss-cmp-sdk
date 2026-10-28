export default ({sdk}) => ({
  capabilities: {
    HOST_EVENTS: () => typeof sdk.graniteEvent?.addEventListener === 'function' && typeof sdk.SafeArea?.subscribe === 'function' && typeof sdk.SafeArea?.get === 'function',
  },
  streams: {
    hostEvents: () => ({emit, fail}) => {
      if (typeof sdk.graniteEvent?.addEventListener !== 'function' || typeof sdk.SafeArea?.subscribe !== 'function' || typeof sdk.SafeArea?.get !== 'function') throw {code:'UNSUPPORTED'};
      const removers = [];
      const cleanup = () => { for (const remove of removers.splice(0)) { try { remove(); } catch {} } };
      try {
        for (const [name, type] of [['backEvent','back'], ['homeEvent','home']]) {
          removers.push(sdk.graniteEvent.addEventListener(name, {onEvent: () => emit({type}), onError: fail}));
        }
        removers.push(sdk.SafeArea.subscribe({onEvent: insets => emit({type:'safeArea', ...insets})}));
        emit({type:'safeArea', ...sdk.SafeArea.get()});
      } catch (error) { cleanup(); throw error; }
      return cleanup;
    },
  },
});
